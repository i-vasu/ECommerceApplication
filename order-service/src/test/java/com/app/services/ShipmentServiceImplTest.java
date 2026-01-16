package com.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.entites.Address;
import com.app.entites.Order;
import com.app.entites.OrderItem;
import com.app.entites.Shipment;
import com.app.entites.User;
import com.app.exceptions.ResourceNotFoundException;
import com.app.repositories.OrderRepo;
import com.app.repositories.ShipmentRepo;
import com.app.repositories.UserRepo;

class ShipmentServiceImplTest {

    @Mock
    private ShiprocketService shiprocketService;

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
        ReflectionTestUtils.setField(shipmentService, "shippingProvider", "SHIPROCKET");
    }

    @Test
    @DisplayName("Create Shipment - Success with Shiprocket")
    void testCreateShipment_Success() {
        Long orderId = 1L;
        String email = "test@example.com";

        Order order = new Order();
        order.setOrderId(orderId);
        order.setEmail(email);
        order.setTotalAmount(1000.0);
        order.setOrderDate(LocalDate.now());

        OrderItem item = new OrderItem();
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setOrderedProductPrice(500.0);
        order.setOrderItems(List.of(item));

        User user = new User();
        user.setEmail(email);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setMobileNumber("9876543210");

        Address address = new Address("India", "Karnataka", "Bangalore", "560001", "MG Road", "Building A");
        user.setAddresses(Collections.singletonList(address));

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));

        // Mock Shiprocket response
        Map<String, Object> shiprocketResponse = new HashMap<>();
        shiprocketResponse.put("order_id", 12345);
        shiprocketResponse.put("shipment_id", 67890);
        shiprocketResponse.put("status", "NEW");
        when(shiprocketService.createShipment(any(Order.class))).thenReturn(shiprocketResponse);

        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment createdShipment = shipmentService.createShipment(orderId);

        assertNotNull(createdShipment);
        assertEquals("Shiprocket", createdShipment.getCarrier());
        assertEquals("CREATED", createdShipment.getStatus());
        assertEquals("12345", createdShipment.getExternalOrderId());
        assertEquals("67890", createdShipment.getExternalShipmentId());

        verify(shiprocketService).createShipment(any(Order.class));
        verify(shipmentRepo).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Create Shipment - Order Already Has Shipment")
    void testCreateShipment_AlreadyExists() {
        Long orderId = 1L;

        Shipment existingShipment = new Shipment();
        existingShipment.setShipmentId(10L);
        existingShipment.setAwbNumber("EXISTING123");

        Order order = new Order();
        order.setOrderId(orderId);
        order.setShipment(existingShipment);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        Shipment result = shipmentService.createShipment(orderId);

        assertEquals(existingShipment, result);
        assertEquals("EXISTING123", result.getAwbNumber());
    }

    @Test
    @DisplayName("Create Shipment - Order Not Found")
    void testCreateShipment_OrderNotFound() {
        Long orderId = 999L;
        when(orderRepo.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> shipmentService.createShipment(orderId));
    }

    @Test
    @DisplayName("Track Shipment - Success with AWB")
    void testTrackShipment_SuccessWithAwb() {
        Long shipmentId = 1L;
        String awb = "67890";

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setAwbNumber(awb);
        shipment.setCarrier("Shiprocket");

        when(shipmentRepo.findById(shipmentId)).thenReturn(Optional.of(shipment));

        Map<String, Object> trackingResponse = new HashMap<>();
        trackingResponse.put("tracking_data", Map.of("shipment_status", "In Transit"));
        when(shiprocketService.trackByAwb(eq(awb))).thenReturn(trackingResponse);

        Map<String, Object> result = shipmentService.trackShipment(shipmentId);

        assertNotNull(result);
        assertNotNull(result.get("tracking_data"));
        verify(shiprocketService).trackByAwb(eq(awb));
    }

    @Test
    @DisplayName("Track Shipment - Success with Shipment ID")
    void testTrackShipment_SuccessWithShipmentId() {
        Long shipmentId = 1L;
        String externalShipmentId = "12345";

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setAwbNumber(null); // No AWB yet
        shipment.setExternalShipmentId(externalShipmentId);
        shipment.setCarrier("Shiprocket");

        when(shipmentRepo.findById(shipmentId)).thenReturn(Optional.of(shipment));

        Map<String, Object> trackingResponse = new HashMap<>();
        trackingResponse.put("tracking_data", Map.of("shipment_status", "Processing"));
        when(shiprocketService.trackByShipmentId(eq(externalShipmentId))).thenReturn(trackingResponse);

        Map<String, Object> result = shipmentService.trackShipment(shipmentId);

        assertNotNull(result);
        verify(shiprocketService).trackByShipmentId(eq(externalShipmentId));
    }

    @Test
    @DisplayName("Track Shipment - Shipment Not Found")
    void testTrackShipment_ShipmentNotFound() {
        Long shipmentId = 999L;
        when(shipmentRepo.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> shipmentService.trackShipment(shipmentId));
    }

    @Test
    @DisplayName("Generate AWB - Success")
    void testGenerateAwb_Success() {
        Long shipmentId = 1L;
        String externalShipmentId = "67890";
        String courierId = "1";

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setCarrier("Shiprocket");
        shipment.setExternalShipmentId(externalShipmentId);

        when(shipmentRepo.findById(shipmentId)).thenReturn(Optional.of(shipment));

        Map<String, Object> awbResponse = new HashMap<>();
        awbResponse.put("awb_code", "AWB123456");
        awbResponse.put("courier_name", "Delhivery");
        when(shiprocketService.generateAwb(eq(externalShipmentId), eq(courierId))).thenReturn(awbResponse);

        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = shipmentService.generateAwb(shipmentId, courierId);

        assertEquals("AWB123456", result.getAwbNumber());
        assertEquals("Delhivery", result.getCourierName());
        assertEquals("AWB_ASSIGNED", result.getStatus());
    }

    @Test
    @DisplayName("Request Pickup - Success")
    void testRequestPickup_Success() {
        Long shipmentId = 1L;
        String externalShipmentId = "67890";

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setCarrier("Shiprocket");
        shipment.setExternalShipmentId(externalShipmentId);

        when(shipmentRepo.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shiprocketService.requestPickup(eq(externalShipmentId))).thenReturn(new HashMap<>());
        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = shipmentService.requestPickup(shipmentId);

        assertEquals("PICKUP_REQUESTED", result.getStatus());
        verify(shiprocketService).requestPickup(eq(externalShipmentId));
    }

    @Test
    @DisplayName("Cancel Shipment - Success")
    void testCancelShipment_Success() {
        Long shipmentId = 1L;
        String externalOrderId = "12345";

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setCarrier("Shiprocket");
        shipment.setExternalOrderId(externalOrderId);

        when(shipmentRepo.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shiprocketService.cancelOrder(any())).thenReturn(new HashMap<>());
        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        shipmentService.cancelShipment(shipmentId);

        assertEquals("CANCELLED", shipment.getStatus());
        verify(shiprocketService).cancelOrder(any());
        verify(shipmentRepo).save(any(Shipment.class));
    }
}
