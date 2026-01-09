package com.app.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ERPNextServiceTest {

    @Autowired
    private ERPNextService erpNextService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void testSyncItemsHandshake() {
        // Prepare mock response
        Map<String, Object> body = new HashMap<>();
        body.put("data", Collections.emptyList());
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

        // Mock RestTemplate behavior
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // Trigger sync
        erpNextService.syncItems();

        // Verify that the restTemplate was called with the correct headers (Token auth)
        verify(restTemplate, atLeastOnce()).exchange(
                contains("api/resource/Item"),
                eq(HttpMethod.GET),
                argThat(entity -> entity.getHeaders().get("Authorization").get(0).startsWith("token ")),
                eq(Map.class));
    }
}
