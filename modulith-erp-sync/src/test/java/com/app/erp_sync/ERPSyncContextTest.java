package com.app.erp_sync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.app.erp_sync.listeners.ERPEventListener;
import com.app.erp_sync.gateway.ERPNextSyncConfig;
import com.app.erp_sync.gateway.ERPNextService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class ERPSyncContextTest {

    @SpringBootApplication(scanBasePackages = "com.app.erp_sync")
    static class TestApp {}

    @MockitoBean
    private ERPNextService erpNextService;

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
