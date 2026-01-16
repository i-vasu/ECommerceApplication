package com.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

import com.app.payloads.ProductDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.SearchResult;

public class SearchServiceImplTest {

    @Mock
    private Client meilisearchClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SearchServiceImpl searchService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSearchProducts() throws Exception {
        String query = "test";
        Index index = mock(Index.class);
        SearchResult searchResult = mock(SearchResult.class);

        when(meilisearchClient.index(anyString())).thenReturn(index);
        when(index.search(query)).thenReturn(searchResult);

        java.util.ArrayList<java.util.HashMap<String, Object>> hits = new java.util.ArrayList<>();
        java.util.HashMap<String, Object> hit = new java.util.HashMap<>();
        hit.put("productId", 1L);
        hit.put("productName", "Test Product");
        hits.add(hit);

        when(searchResult.getHits()).thenReturn(hits);

        ProductDTO dto = new ProductDTO();
        dto.setProductId(1L);
        dto.setProductName("Test Product");

        when(objectMapper.convertValue(hit, ProductDTO.class)).thenReturn(dto);

        List<ProductDTO> results = searchService.searchProducts(query);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Product", results.get(0).getProductName());
    }
}
