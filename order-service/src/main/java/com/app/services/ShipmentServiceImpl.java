package com.app.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.entites.Order;
import com.app.entites.Shipment;
import com.app.exceptions.ResourceNotFoundException;
import com.app.external.ShadowfaxClient;
import com.app.repositories.OrderRepo;
import com.app.repositories.ShipmentRepo;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    @Autowired
    private ShadowfaxClient shadowfaxClient;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ShipmentRepo shipmentRepo;

    @Autowired
    private com.app.repositories.UserRepo userRepo;

    @Value("${shadowfax.token:Token token_value}")
    private String shadowfaxToken;

    @Override
    @Transactional
    public Shipment createShipment(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        if (order.getShipment() != null) {
            return order.getShipment();
        }

        Map<String, Object> payload = buildShadowfaxPayload(order);

        Map<String, Object> response = shadowfaxClient.createOrder(shadowfaxToken, payload);

        // Parse response to get AWB
        // Assuming response structure: {"data": {"awb_number": "...", "status": "..."}}
        // This depends on actual API response, implementing basic extraction

        String awb = null;
        String status = "CREATED";

        if (response.containsKey("data") && response.get("data") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            awb = (String) data.get("awb_number");
            status = (String) data.get("status");
        } else if (response.containsKey("awb_number")) {
            awb = (String) response.get("awb_number");
        }

        if (awb == null && response.containsKey("message")) {
            // Handle error or store failure
            throw new RuntimeException("Shadowfax Error: " + response.get("message"));
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setCarrier("Shadowfax");
        shipment.setAwbNumber(awb);
        shipment.setStatus(status);

        order.setShipment(shipment);

        return shipmentRepo.save(shipment);
    }

    @Override
    public Map<String, Object> trackShipment(Long shipmentId) {
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        return shadowfaxClient.trackOrder(shadowfaxToken, shipment.getAwbNumber());
    }

    private Map<String, Object> buildShadowfaxPayload(Order order) {
        com.app.entites.User user = userRepo.findByEmail(order.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", order.getEmail()));

        if (user.getAddresses().isEmpty()) {
            throw new RuntimeException("No address found for user to create shipment");
        }

        com.app.entites.Address address = user.getAddresses().get(0);

        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> orderDetails = new HashMap<>();
        orderDetails.put("client_order_id", String.valueOf(order.getOrderId()));
        orderDetails.put("actual_weight", 0.5); // Placeholder
        orderDetails.put("product_value", order.getTotalAmount());
        orderDetails.put("payment_mode", "prepaid"); // Or COD based on payment logic

        Map<String, Object> consigneeDetails = new HashMap<>();
        consigneeDetails.put("city", address.getCity());
        consigneeDetails.put("name", user.getFirstName() + " " + user.getLastName());
        consigneeDetails.put("phone", user.getMobileNumber());
        consigneeDetails.put("address_line_1", address.getBuildingName() + ", " + address.getStreet());
        consigneeDetails.put("pincode", address.getPincode());
        consigneeDetails.put("state", address.getState());
        consigneeDetails.put("country", address.getCountry());

        Map<String, Object> pickupDetails = new HashMap<>();
        pickupDetails.put("warehouse_name", "Main Warehouse");
        pickupDetails.put("city", "Bangalore");
        pickupDetails.put("address_line_1", "Warehouse Address");
        pickupDetails.put("pincode", "560001");
        pickupDetails.put("name", "Merchant");
        pickupDetails.put("phone", "9999999999");

        payload.put("order_details", orderDetails);
        payload.put("consignee_details", consigneeDetails);
        payload.put("pickup_details", pickupDetails);

        return payload;
    }

}
