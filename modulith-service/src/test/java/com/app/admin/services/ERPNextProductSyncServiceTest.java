package com.app.admin.services;

import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.repositories.ProductVariantRepo;
import com.app.core.multitenancy.ERPNextCredentialProvider;
import com.app.core.multitenancy.Tenant;
import com.app.erp_sync.gateway.SyncGateway;
import com.app.erp_sync.services.ERPNextProductSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ERPNextProductSyncServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private ProductVariantRepo variantRepo;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ERPNextCredentialProvider credentialProvider;

    @Mock
    private SyncGateway syncGateway;

    @InjectMocks
    private ERPNextProductSyncService syncService;

    @Test
    void syncItems_shouldInvokeGateway_withValidCredentials() {
        // Arrange
        Tenant tenant = new Tenant();
        tenant.setTenantId("t1");
        tenant.setErpNextUrl("http://test.com");
        tenant.setErpNextApiKey("key");
        tenant.setErpNextApiSecret("secret");

        // Act
        syncService.syncItems(tenant);

        // Assert
        verify(syncGateway).startSync(eq("MANUAL_TRIGGER_t1"),
                eq("http://test.com/api/resource/Item"),
                eq("key"),
                eq("secret"));
    }

    @Test
    void syncItems_shouldFallbackToDefaults_whenTenantConfigMissing() {
        // Arrange
        Tenant tenant = new Tenant();
        tenant.setTenantId("t2");
        // missing url/keys

        when(credentialProvider.getApiKey()).thenReturn("gKey");
        when(credentialProvider.getApiSecret()).thenReturn("gSecret");
        when(credentialProvider.getBaseUrl()).thenReturn("http://global.com");

        // Act
        syncService.syncItems(tenant);

        // Assert
        verify(syncGateway).startSync(eq("MANUAL_TRIGGER_t2"),
                eq("http://global.com/api/resource/Item"),
                eq("gKey"),
                eq("gSecret"));
    }

    @Test
    void syncItems_shouldFixUrl_whenMissingProtocol() {
        // Arrange
        Tenant tenant = new Tenant();
        tenant.setTenantId("t3");
        tenant.setErpNextUrl("localhost:8000"); // Missing http
        tenant.setErpNextApiKey("k");
        tenant.setErpNextApiSecret("s");

        // Act
        syncService.syncItems(tenant);

        // Assert: Check that http:// is prepended
        verify(syncGateway).startSync(eq("MANUAL_TRIGGER_t3"),
                eq("http://localhost:8000/api/resource/Item"),
                eq("k"),
                eq("s"));
    }
}
