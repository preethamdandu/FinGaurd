package com.fingaurd.config;

import com.fingaurd.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP interceptor that logs every API request to MongoDB via the
 * Python fraud-detection service's audit endpoint (US-016).
 *
 * The audit document is sent fire-and-forget so it never delays the
 * response to the client.
 */
@Component
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {

    @Value("${fraud-detection.service.url:http://localhost:8000}")
    private String fraudServiceUrl;

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            String userId = null;
            String email = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                userId = principal.getId().toString();
                email = principal.getEmail();
            }

            Map<String, Object> auditEntry = new HashMap<>();
            auditEntry.put("userId", userId);
            auditEntry.put("email", email);
            auditEntry.put("method", request.getMethod());
            auditEntry.put("path", request.getRequestURI());
            auditEntry.put("queryString", request.getQueryString());
            auditEntry.put("statusCode", response.getStatus());
            auditEntry.put("ipAddress", getClientIp(request));
            auditEntry.put("userAgent", request.getHeader(HttpHeaders.USER_AGENT));
            auditEntry.put("timestamp", Instant.now().toString());

            // Fire-and-forget POST to the Python service's audit endpoint
            WebClient.create(fraudServiceUrl)
                    .post()
                    .uri("/api/audit")
                    .bodyValue(auditEntry)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            ok -> {},
                            err -> log.debug("Audit log post failed (non-critical): {}", err.getMessage())
                    );

        } catch (Exception e) {
            // Never let audit logging break the request
            log.debug("Audit interceptor error (non-critical): {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
