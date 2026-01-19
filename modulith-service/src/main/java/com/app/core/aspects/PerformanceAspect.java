package com.app.core.aspects;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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
public class PerformanceAspect {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PerformanceAspect.class);

    private final ObservationRegistry observationRegistry;

    public PerformanceAspect(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    /**
     * Profiles methods in service and controller layers across all modules.
     * Matches: com.app.{module}.services.*, com.app.{module}.controllers.*, etc.
     */
    @Around("execution(* com.app..services..*(..)) || execution(* com.app..controllers..*(..)) || execution(@org.springframework.web.bind.annotation.RestController * *(..))")
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
