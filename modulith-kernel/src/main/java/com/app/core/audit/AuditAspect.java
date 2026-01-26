package com.app.core.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Log4j2
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepo auditLogRepo;

    @AfterReturning(value = "@annotation(auditTrail)", returning = "result")
    public void logAction(JoinPoint joinPoint, AuditTrail auditTrail, Object result) {
        String userEmail = "anonymous";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            userEmail = authentication.getName();
        }

        HttpServletRequest request = null;
        String ipAddress = "unknown";
        if (RequestContextHolder.getRequestAttributes() != null) {
            request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            ipAddress = request.getRemoteAddr();
        }

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String details = "Method: " + joinPoint.getSignature().getName() + " | Args: "
                + maskSensitiveArgs(joinPoint.getArgs());
        if (details.length() > 2000) {
            details = details.substring(0, 1997) + "...";
        }

        AuditLog auditLog = new AuditLog(userEmail, auditTrail.action(), details, ipAddress, tenantId);
        auditLogRepo.save(auditLog);

        log.info("Audit Log Saved: {} - {} by {} [Tenant: {}]", auditTrail.action(), joinPoint.getSignature().getName(),
                userEmail, tenantId);
    }

    private String maskSensitiveArgs(Object[] args) {
        if (args == null)
            return "[]";
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null)
                        return "null";
                    String str = arg.toString();
                    // Basic masking for common sensitive keywords
                    if (str.toLowerCase().contains("password") || str.toLowerCase().contains("secret") ||
                            str.toLowerCase().contains("token") || str.toLowerCase().contains("key")) {
                        return "[MASKED]";
                    }
                    return str;
                })
                .collect(java.util.stream.Collectors.toList())
                .toString();
    }
}
