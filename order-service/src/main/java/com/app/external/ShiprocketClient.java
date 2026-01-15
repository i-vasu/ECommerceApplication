package com.app.external;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Shiprocket API Feign Client
 * 
 * Production-ready client for Shiprocket logistics platform.
 * API Documentation: https://apiv2.shiprocket.in/v1/external/
 * 
 * Authentication:
 * - JWT token obtained via /auth/login endpoint
 * - Token valid for 10 days (240 hours)
 * - All requests require: Authorization: Bearer <token>
 */
@FeignClient(name = "shiprocket-client", url = "${shiprocket.api.url:https://apiv2.shiprocket.in/v1/external}")
public interface ShiprocketClient {

    /**
     * Authenticate with Shiprocket to get JWT token
     * Token is valid for 10 days
     * 
     * @param credentials Map containing "email" and "password"
     * @return Response with "token" field containing JWT
     */
    @PostMapping("/auth/login")
    Map<String, Object> login(@RequestBody Map<String, String> credentials);

    /**
     * Create an ad-hoc (quick custom) order
     * Does not require pre-existing product catalog
     * 
     * @param authorization Bearer token: "Bearer <token>"
     * @param orderPayload  Complete order details
     * @return Response with order_id, shipment_id, status
     */
    @PostMapping("/orders/create/adhoc")
    Map<String, Object> createOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> orderPayload);

    /**
     * Generate AWB (Air Waybill) and assign courier
     * 
     * @param authorization Bearer token
     * @param payload       Contains shipment_id and courier_id
     * @return Response with awb_code, courier_name
     */
    @PostMapping("/courier/assign/awb")
    Map<String, Object> generateAwb(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> payload);

    /**
     * Request pickup for shipment
     * 
     * @param authorization Bearer token
     * @param payload       Contains shipment_id
     * @return Pickup request confirmation
     */
    @PostMapping("/courier/generate/pickup")
    Map<String, Object> requestPickup(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> payload);

    /**
     * Track shipment by AWB number
     * 
     * @param authorization Bearer token
     * @param awbNumber     Air Waybill number
     * @return Tracking details including current status and scan history
     */
    @GetMapping("/courier/track/awb/{awbNumber}")
    Map<String, Object> trackByAwb(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("awbNumber") String awbNumber);

    /**
     * Track shipment by Shiprocket shipment ID
     */
    @GetMapping("/courier/track/shipment/{shipmentId}")
    Map<String, Object> trackByShipmentId(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("shipmentId") String shipmentId);

    /**
     * Get list of available courier partners and their rates
     * 
     * @param authorization Bearer token
     * @param payload       Contains pickup_postcode, delivery_postcode, weight, cod
     *                      (0/1)
     * @return List of available couriers with rates
     */
    @GetMapping("/courier/serviceability")
    Map<String, Object> getAvailableCouriers(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> payload);

    /**
     * Cancel an order
     * 
     * @param authorization Bearer token
     * @param payload       Contains ids array of order IDs to cancel
     * @return Cancellation confirmation
     */
    @PostMapping("/orders/cancel")
    Map<String, Object> cancelOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> payload);

    /**
     * Get order details by order ID
     */
    @GetMapping("/orders/show/{orderId}")
    Map<String, Object> getOrder(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("orderId") String orderId);

    /**
     * Get all pickup locations configured in account
     */
    @GetMapping("/settings/company/pickup")
    Map<String, Object> getPickupLocations(
            @RequestHeader("Authorization") String authorization);

    /**
     * Generate shipping label for a shipment
     */
    @PostMapping("/courier/generate/label")
    Map<String, Object> generateLabel(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> payload);

    /**
     * Generate invoice for a shipment
     */
    @PostMapping("/orders/print/invoice")
    Map<String, Object> generateInvoice(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> payload);
}
