package com.app.core.multitenancy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.jspecify.annotations.NonNull;

@Configuration
@RequiredArgsConstructor
public class MultiTenancyWebConfig implements WebMvcConfigurer {

    // Filter-based multitenancy is now handled by TenantFilter (ScopedValue)
    // No interceptors required.
}
