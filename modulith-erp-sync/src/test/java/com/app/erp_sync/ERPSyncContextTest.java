package com.app.erp_sync;

import com.app.core.async.EventProducer;
import com.app.core.config.HttpClientConfig;
import com.app.core.multitenancy.ERPNextCredentialProvider;
import com.app.core.services.RedisLockService;
import com.app.erp_sync.gateway.ERPNextSyncConfig;
import com.app.erp_sync.listeners.ERPEventListener;
import com.app.logistics.repositories.ShipmentRepo;
import com.app.order.repositories.OrderRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class ERPSyncContextTest {

    @Configuration
    @ComponentScan(basePackages = "com.app.erp_sync")
    @Import({ERPNextSyncConfig.class, HttpClientConfig.class})
    static class TestConfig {
        @Bean
        @Primary
        public ERPNextCredentialProvider credentialProvider() { return org.mockito.Mockito.mock(ERPNextCredentialProvider.class); }
        @Bean
        public StringRedisTemplate redisTemplate() { return org.mockito.Mockito.mock(StringRedisTemplate.class); }
        @Bean
        public OrderRepo orderRepo() { return org.mockito.Mockito.mock(OrderRepo.class); }
        @Bean
        public ShipmentRepo shipmentRepo() { return org.mockito.Mockito.mock(ShipmentRepo.class); }
        @Bean
        public EventProducer eventProducer() { return org.mockito.Mockito.mock(EventProducer.class); }
        @Bean
        public RedisLockService lockService() { return org.mockito.Mockito.mock(RedisLockService.class); }
        @Bean
        public ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean
        public com.app.catalog.repositories.ProductVariantRepo productVariantRepo() { return org.mockito.Mockito.mock(com.app.catalog.repositories.ProductVariantRepo.class); }
        @Bean
        public com.app.catalog.repositories.CategoryRepo categoryRepo() { return org.mockito.Mockito.mock(com.app.catalog.repositories.CategoryRepo.class); }
        @Bean
        public org.springframework.cache.CacheManager cacheManager() { return org.mockito.Mockito.mock(org.springframework.cache.CacheManager.class); }
        @Bean
        public com.app.core.multitenancy.TenantManagementService tenantService() { return org.mockito.Mockito.mock(com.app.core.multitenancy.TenantManagementService.class); }
        @Bean
        public com.app.core.multitenancy.TenantRepository tenantRepository() { return org.mockito.Mockito.mock(com.app.core.multitenancy.TenantRepository.class); }
        @Bean
        public org.springframework.jdbc.core.JdbcTemplate jdbcTemplate() { return org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class); }
    }

    @Autowired
    private ERPEventListener erpEventListener;

    @Autowired
    private ERPNextSyncConfig erpNextSyncConfig;

    @Test
    void verifiesContextLoads() {
        assertThat(erpEventListener).isNotNull();
        assertThat(erpNextSyncConfig).isNotNull();
    }
}
