package com.app.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class ContextLoadTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Just verify if the context loads successfully
    }
}
