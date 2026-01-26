package com.app.search.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VisualSearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private VisualSearchService visualSearchService;

    @Test
    public void testUpdateProductVector_Success() {
        // Mock DB Update
        when(jdbcTemplate.update(anyString(), anyString(), anyLong())).thenReturn(1);

        // Mock RestClient for Image Download
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        
        // Valid 1x1 GIF to satisfy ImageFactory
        byte[] validImageBytes = java.util.Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

        when(responseSpec.body(byte[].class)).thenReturn(validImageBytes);

        // Mock DJL Model and Predictor to bypass native loading
        ai.djl.repository.zoo.ZooModel mockModel = mock(ai.djl.repository.zoo.ZooModel.class);
        ai.djl.inference.Predictor mockPredictor = mock(ai.djl.inference.Predictor.class);

        try {
            when(mockModel.newPredictor()).thenReturn(mockPredictor);
            // Mock prediction result (512-dim vector)
            when(mockPredictor.predict(any(ai.djl.modality.cv.Image.class))).thenReturn(new float[512]);
        } catch (Exception e) {
            throw new RuntimeException("Mock setup failed", e);
        }

        // Inject mock model into service
        org.springframework.test.util.ReflectionTestUtils.setField(visualSearchService, "model", mockModel);

        // Execute Flow
        visualSearchService.updateProductVector(100L, "http://example.com/image.jpg");

        // Verify RestClient was called
        verify(restClient).get();
        
        // Verify DB update was called (Flow completed success)
        // (sql, args...) -> update(sql, stringVector, id)
        verify(jdbcTemplate).update(
            contains("UPDATE products SET feature_vector"), 
            anyString(), 
            eq(100L)
        );
    }
}
