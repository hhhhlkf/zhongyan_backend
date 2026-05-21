package com.zhongyan.uav.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.common.response.ApiResult;
import com.zhongyan.uav.common.security.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenService jwtTokenService,
                                                   ObjectMapper objectMapper,
                                                   @Value("${bms.security.rate-limit.enabled:true}") boolean rateLimitEnabled,
                                                   @Value("${bms.security.rate-limit.window-seconds:60}") long rateLimitWindowSeconds,
                                                   @Value("${bms.security.rate-limit.max-requests:120}") int rateLimitMaxRequests)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/auth/token").permitAll()
                        .requestMatchers("/approvals/**").hasAnyAuthority("TASK_APPROVE", "ROLE_ADMIN")
                        .requestMatchers("/agent/tool-calls/*/approve", "/agent/tool-calls/*/reject")
                        .hasAnyAuthority("AGENT_TOOL_APPROVE", "ROLE_ADMIN")
                        .requestMatchers("/agent/**").hasAnyAuthority("AGENT_CHAT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        .anyRequest().hasAnyAuthority("WRITE", "ROLE_ADMIN"))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenService), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new ApiRateLimitFilter(objectMapper, rateLimitEnabled,
                        rateLimitWindowSeconds, rateLimitMaxRequests), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new ApiAuditLogFilter(), AuthorizationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeFailure(response, objectMapper, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                writeFailure(response, objectMapper, ErrorCode.FORBIDDEN)))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
                                                 @Value("${spring.security.user.name:admin}") String username,
                                                 @Value("${spring.security.user.password:admin}") String password) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .passwordEncoder(passwordEncoder::encode)
                .password(password)
                .authorities("ROLE_ADMIN", "READ", "WRITE", "TASK_APPROVE", "AGENT_CHAT", "AGENT_TOOL_APPROVE")
                .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    private static void writeFailure(HttpServletResponse response, ObjectMapper objectMapper,
                                     ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResult.failure(errorCode.code(), errorCode.message()));
    }

    private static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtTokenService jwtTokenService;

        private JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
            this.jwtTokenService = jwtTokenService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    Authentication authentication = jwtTokenService.parseAuthentication(authorization.substring(7));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                }
            }
            filterChain.doFilter(request, response);
        }
    }

    private static class ApiRateLimitFilter extends OncePerRequestFilter {
        private static final int MAX_BUCKETS = 4096;

        private final ObjectMapper objectMapper;
        private final boolean enabled;
        private final long windowMillis;
        private final int maxRequests;
        private final Clock clock;
        private final Map<String, RateBucket> buckets = new LinkedHashMap<>(128, 0.75f, true);

        private ApiRateLimitFilter(ObjectMapper objectMapper, boolean enabled, long windowSeconds, int maxRequests) {
            this.objectMapper = objectMapper;
            this.enabled = enabled;
            this.windowMillis = Math.max(1, windowSeconds) * 1000;
            this.maxRequests = Math.max(1, maxRequests);
            this.clock = Clock.systemUTC();
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            if (!enabled || isPublicEndpoint(request) || allow(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeFailure(response, objectMapper, ErrorCode.TOO_MANY_REQUESTS);
        }

        private boolean allow(HttpServletRequest request) {
            long now = clock.millis();
            String key = rateLimitKey(request);
            synchronized (buckets) {
                RateBucket bucket = buckets.get(key);
                if (bucket == null || now >= bucket.windowStartedAt + windowMillis) {
                    buckets.put(key, new RateBucket(now, 1));
                    trimBuckets();
                    return true;
                }
                if (bucket.count >= maxRequests) {
                    return false;
                }
                bucket.count++;
                return true;
            }
        }

        private String rateLimitKey(HttpServletRequest request) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String principal = authentication == null ? "anonymous" : authentication.getName();
            return principal + "|" + request.getRemoteAddr();
        }

        private void trimBuckets() {
            while (buckets.size() > MAX_BUCKETS) {
                String eldest = buckets.keySet().iterator().next();
                buckets.remove(eldest);
            }
        }

        private boolean isPublicEndpoint(HttpServletRequest request) {
            String path = request.getRequestURI();
            return HttpMethod.OPTIONS.matches(request.getMethod())
                    || path.endsWith("/actuator/health")
                    || path.endsWith("/auth/token");
        }

        private static class RateBucket {
            private final long windowStartedAt;
            private int count;

            private RateBucket(long windowStartedAt, int count) {
                this.windowStartedAt = windowStartedAt;
                this.count = count;
            }
        }
    }

    private static class ApiAuditLogFilter extends OncePerRequestFilter {
        private static final Logger log = LoggerFactory.getLogger(ApiAuditLogFilter.class);

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            long started = System.nanoTime();
            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            response.setHeader("X-Request-Id", requestId);
            try {
                filterChain.doFilter(request, response);
            } finally {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String principal = authentication == null ? "anonymous" : authentication.getName();
                long durationMs = (System.nanoTime() - started) / 1_000_000;
                log.info("api_audit requestId={} method={} path={} status={} principal={} remote={} durationMs={}",
                        requestId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                        principal, request.getRemoteAddr(), durationMs);
            }
        }
    }
}

