package com.app.services;

import com.app.entites.Category;
import com.app.entites.Product;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ERPNextServiceSyncTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CategoryRepo categoryRepo;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ERPNextService erpNextService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(erpNextService, "erpNextUrl", "http://erpnext.com/api/resource/Item");
        ReflectionTestUtils.setField(erpNextService, "apiKey", "key");
        ReflectionTestUtils.setField(erpNextService, "apiSecret", "secret");
    }

    @Test
    void testSyncItems_SuccessfulSync() {
        // Mock API Response
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("item_name", "Test Item");
        itemData.put("description", "Test Description");
        itemData.put("standard_rate", 100.0);
        itemData.put("item_group", "Fashion");
        itemData.put("image", "/files/test.jpg");

        List<Map<String, Object>> dataList = new ArrayList<>();
        dataList.add(itemData);

        Map<String, Object> body = new HashMap<>();
        body.put("data", dataList);

        ResponseEntity<Map> response = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        // Mock Category
        Category category = new Category();
        category.setCategoryName("Fashion");
        when(categoryRepo.findByCategoryName("Fashion")).thenReturn(category);

        // Mock Product
        when(productRepo.findByProductName("Test Item")).thenReturn(null);

        // Mock MinIO Mirroring
        when(minioService.uploadFromUrl(anyString(), anyString())).thenReturn("minio_test_abc.jpg");

        erpNextService.syncItems();

        verify(productRepo, times(1)).save(any(Product.class));
        verify(minioService, times(1)).uploadFromUrl(contains("/files/test.jpg"), anyString());
    }

    @Test
    void testSyncItems_ERPNextDown() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        erpNextService.syncItems();

        verify(productRepo, never()).save(any(Product.class));
    }
}
