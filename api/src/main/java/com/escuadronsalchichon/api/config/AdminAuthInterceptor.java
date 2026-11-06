package com.escuadronsalchichon.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final byte[] configuredToken;

    public AdminAuthInterceptor(@Value("${salchichon.admin.token:}") String token) {
        this.configuredToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (configuredToken.length < 32) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Admin authentication is not configured");
            return false;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        byte[] supplied = header.substring(7).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(configuredToken, supplied)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        return true;
    }
}
