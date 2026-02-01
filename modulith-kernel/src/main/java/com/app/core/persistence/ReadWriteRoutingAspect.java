package com.app.core.persistence;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@Order(0) // Run before TransactionInterceptor
public class ReadWriteRoutingAspect {

    @Around("@annotation(transactional)")
    public Object proceed(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        try {
            if (transactional.readOnly()) {
                TransactionRoutingDataSource.setReadonly(true);
            } else {
                TransactionRoutingDataSource.setReadonly(false);
            }
            return pjp.proceed();
        } finally {
            TransactionRoutingDataSource.clear();
        }
    }
}
