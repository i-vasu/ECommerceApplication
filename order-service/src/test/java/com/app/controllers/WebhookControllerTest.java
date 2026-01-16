package com.app.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.app.services.PaymentService;
import com.razorpay.Utils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @BeforeEach
    public void setup() {
    }

    @Test
    public void testHandleRazorpayWebhookSuccess() throws Exception {
        String payload = new JSONObject()
                .put("event", "payment.captured")
                .put("payload", new JSONObject()
                        .put("payment", new JSONObject()
                                .put("entity", new JSONObject()
                                        .put("order_id", "order_123")
                                        .put("id", "pay_456"))))
                .toString();

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyWebhookSignature(anyString(), anyString(), anyString()))
                    .thenReturn(true);

            doNothing().when(paymentService).processPaymentCapture("order_123", "pay_456");

            mockMvc.perform(post("/api/public/webhooks/razorpay")
                    .header("X-Razorpay-Signature", "some_sig")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            verify(paymentService).processPaymentCapture("order_123", "pay_456");
        }
    }
}
