package com.app.aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Performance Aspect
 * Logs execution time of methods annotated with @LogExecutionTime
 * or automatically logs Service/Controller execution times.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PerformanceAspect {

    private final ObservationRegistry observationRegistry;

    @Around("execution(* com.app.services..*(..)) || execution(* com.app.controller..*(..))")
    public Object profile(ProceedingJoinPoint joinPoint) throws Throwable {
        return Observation.createNotStarted(joinPoint.getSignature().getName(), observationRegistry)
                .observeChecked(() -> {
                    long start = System.currentTimeMillis();
                    Object proceed = joinPoint.proceed();
                    long executionTime = System.currentTimeMillis() - start;

                    // Only log if execution takes longer than 100ms
                    if (executionTime > 100) {
                        log.warn("{} executed in {}ms", joinPoint.getSignature().toShortString(), executionTime);
                    }
                    return proceed;
                });
    }
}
