package com.app.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.app.entites.Category;
import com.app.entites.Product;
import com.app.entites.ProductVariant;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;
import com.app.repositories.ProductVariantRepo;

public class ERPNextProductSyncServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CategoryRepo categoryRepo;

    @Mock
    private ProductVariantRepo variantRepo;

    @InjectMocks
    private ERPNextProductSyncService syncService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(syncService, "erpNextUrl", "http://localhost:8000/api/resource/Item");
        ReflectionTestUtils.setField(syncService, "apiKey", "testKey");
        ReflectionTestUtils.setField(syncService, "apiSecret", "testSecret");
    }

    @Test
    public void testSyncItemsSuccess() {
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("name", "ITEM001");
        item.put("item_name", "Test Item");
        item.put("item_group", "Test Category");
        item.put("standard_rate", 100.0);
        data.add(item);
        responseBody.put("data", data);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Category category = new Category();
        category.setCategoryName("Test Category");
        when(categoryRepo.findByCategoryName("Test Category")).thenReturn(category);
        when(productRepo.findByItemCode("ITEM001")).thenReturn(null);

        syncService.syncItems();

        verify(productRepo, times(1)).save(any(Product.class));
    }

    @Test
    public void testSyncItemsWithVariants() {
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        // Parent item
        Map<String, Object> parent = new HashMap<>();
        parent.put("name", "TSHIRT001");
        parent.put("item_name", "T-Shirt Template");
        parent.put("item_group", "Apparel");
        parent.put("has_variants", 1);
        data.add(parent);

        // Variant item
        Map<String, Object> variant = new HashMap<>();
        variant.put("name", "TSHIRT001-RED-XL");
        variant.put("item_name", "T-Shirt - Red - XL");
        variant.put("variant_of", "TSHIRT001");
        data.add(variant);

        responseBody.put("data", data);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Category category = new Category();
        category.setCategoryName("Apparel");
        when(categoryRepo.findByCategoryName(anyString())).thenReturn(category);

        Product parentProduct = new Product();
        parentProduct.setProductId(1L);
        parentProduct.setItemCode("TSHIRT001");
        when(productRepo.findByItemCode("TSHIRT001")).thenReturn(parentProduct);
        when(variantRepo.findByItemCode("TSHIRT001-RED-XL")).thenReturn(null);

        syncService.syncItems();

        verify(productRepo, times(1)).save(any(Product.class));
        verify(variantRepo, times(1)).save(any(ProductVariant.class));
    }
}
