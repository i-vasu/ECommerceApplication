package com.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.entites.Address;
import com.app.entites.Order;
import com.app.entites.Shipment;
import com.app.entites.User;
import com.app.external.ShadowfaxClient;
import com.app.repositories.OrderRepo;
import com.app.repositories.ShipmentRepo;
import com.app.repositories.UserRepo;

class ShipmentServiceImplTest {

    @Mock
    private ShadowfaxClient shadowfaxClient;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private ShipmentRepo shipmentRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(shipmentService, "shadowfaxToken", "Token test-token");
    }

    @Test
    void testCreateShipment_Success() {
        Long orderId = 1L;
        String email = "test@example.com";

        Order order = new Order();
        order.setOrderId(orderId);
        order.setEmail(email);
        order.setTotalAmount(1000.0);
        order.setOrderDate(LocalDate.now());

        User user = new User();
        user.setEmail(email);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setMobileNumber("9876543210");

        Address address = new Address("India", "Karnataka", "Bangalore", "560001", "MG Road", "Building A");
        user.setAddresses(Collections.singletonList(address));

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));

        Map<String, Object> shadowfaxResponse = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("awb_number", "SF123456");
        data.put("status", "UPLOADED");
        shadowfaxResponse.put("data", data);

        when(shadowfaxClient.createOrder(eq("Token test-token"), anyMap())).thenReturn(shadowfaxResponse);
        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment createdShipment = shipmentService.createShipment(orderId);

        assertNotNull(createdShipment);
        assertEquals("SF123456", createdShipment.getAwbNumber());
        assertEquals("UPLOADED", createdShipment.getStatus());
        assertEquals("Shadowfax", createdShipment.getCarrier());

        verify(shadowfaxClient).createOrder(eq("Token test-token"), anyMap());
        verify(shipmentRepo).save(any(Shipment.class));
    }

    @Test
    void testCreateShipment_UserNoAddress() {
        Long orderId = 1L;
        String email = "noaddress@example.com";

        Order order = new Order();
        order.setOrderId(orderId);
        order.setEmail(email);

        User user = new User();
        user.setEmail(email);
        user.setAddresses(new ArrayList<>()); // Empty addresses

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> shipmentService.createShipment(orderId),
                "No address found for user to create shipment");
    }

    @Test
    void testTrackShipment_Success() {
        Long shipmentId = 1L;
        String awb = "SF123456";

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setAwbNumber(awb);

        when(shipmentRepo.findById(shipmentId)).thenReturn(Optional.of(shipment));

        Map<String, Object> trackingResponse = new HashMap<>();
        trackingResponse.put("status", "In Transit");
        when(shadowfaxClient.trackOrder("Token test-token", awb)).thenReturn(trackingResponse);

        Map<String, Object> result = shipmentService.trackShipment(shipmentId);

        assertNotNull(result);
        assertEquals("In Transit", result.get("status"));
    }
}
