package com.zhongyan.uav.common.security;

import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.common.response.ApiResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/auth/token")
    public ApiResult<Map<String, Object>> issueToken(@RequestBody Map<String, String> request) {
        String username = request == null ? null : request.get("username");
        String password = request == null ? null : request.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ApiResult.failure(ErrorCode.BAD_REQUEST.code(), "username and password are required");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            return ApiResult.success(Map.of(
                    "tokenType", "Bearer",
                    "accessToken", jwtTokenService.createToken(authentication),
                    "expiresIn", jwtTokenService.expirationSeconds()));
        } catch (AuthenticationException ex) {
            return ApiResult.failure(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message());
        }
    }
}
