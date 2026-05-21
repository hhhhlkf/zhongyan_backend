package com.zhongyan.uav.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class JwtTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String issuer;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtTokenService(ObjectMapper objectMapper,
                           @Value("${bms.security.jwt.secret:${jwt.data.SECRET:jwt-token-secret}}") String secret,
                           @Value("${bms.security.jwt.issuer:zhongyan-uav}") String issuer,
                           @Value("${bms.security.jwt.expiration-seconds:${jwt.data.expiration:28800}}") long expirationSeconds) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("JWT secret must contain at least 16 characters");
        }
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(Authentication authentication) {
        Instant now = clock.instant();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", issuer,
                "sub", authentication.getName(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(expirationSeconds).getEpochSecond(),
                "authorities", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .sorted()
                        .toList());
        String unsigned = encodeJson(header) + "." + encodeJson(payload);
        return unsigned + "." + sign(unsigned);
    }

    public Authentication parseAuthentication(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT structure");
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(unsigned).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        Map<String, Object> claims = decodeJson(parts[1]);
        if (!issuer.equals(claims.get("iss"))) {
            throw new IllegalArgumentException("Invalid JWT issuer");
        }
        long exp = ((Number) claims.getOrDefault("exp", 0)).longValue();
        if (clock.instant().getEpochSecond() >= exp) {
            throw new IllegalArgumentException("JWT has expired");
        }
        String username = String.valueOf(claims.get("sub"));
        List<SimpleGrantedAuthority> authorities = authorities(claims.get("authorities"));
        return new UsernamePasswordAuthenticationToken(username, token, authorities);
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private List<SimpleGrantedAuthority> authorities(Object value) {
        if (!(value instanceof Collection<?> rawAuthorities)) {
            return List.of();
        }
        return rawAuthorities.stream()
                .map(String::valueOf)
                .filter(authority -> !authority.isBlank())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encode JWT", ex);
        }
    }

    private Map<String, Object> decodeJson(String encoded) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to decode JWT", ex);
        }
    }

    private String sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }
}
