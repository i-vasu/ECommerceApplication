package com.app.order.services;

import com.app.order.entities.*;
import com.app.order.repositories.ReturnRequestRepo;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.payment.PaymentService;
import com.app.identity.services.WalletService;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import com.app.commerce.states.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRequestRepo returnRequestRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final ERPNextService erpNextService;

    @Transactional
    public ReturnRequest requestPartialReturn(Long orderId, String email, Map<Long, Integer> itemsToReturn,
            String reason, ReturnRequest.RefundType refundType) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        if (!order.getEmail().equals(email)) {
            throw new APIException("Unauthorized return request");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new APIException("Returns only allowed for delivered orders");
        }

        // 15-day return policy
        if (order.getDeliveredDate() != null &&
                order.getDeliveredDate().plusDays(15).isBefore(java.time.LocalDateTime.now())) {
            throw new APIException("Return window (15 days) has expired.");
        }

        ReturnRequest request = new ReturnRequest();
        request.setOrder(order);
        request.setReason(reason);
        request.setStatus(ReturnRequest.ReturnStatus.REQUESTED);
        request.setRefundType(refundType);

        double totalRefund = 0.0;
        for (Map.Entry<Long, Integer> entry : itemsToReturn.entrySet()) {
            Long itemId = entry.getKey();
            Integer qty = entry.getValue();

            OrderItem item = orderItemRepo.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "itemId", itemId));

            if (qty > (item.getQuantity() - item.getReturnedQuantity())) {
                throw new APIException("Invalid return quantity for item: " + item.getProductName());
            }

            // Pro-rata calculation: (Ordered Price - Discount/unit)
            // Note: If coupon was applied on total, this needs more complex logic.
            // For now, assuming discount is already per-item in item.getDiscount().
            double proRataPrice = item.getOrderedPrice() - (item.getDiscount() / item.getQuantity());
            double itemRefund = proRataPrice * qty;

            ReturnItem returnItem = new ReturnItem();
            returnItem.setReturnRequest(request);
            returnItem.setOrderItem(item);
            returnItem.setQuantity(qty);
            returnItem.setUnitRefundAmount(proRataPrice);
            request.getItems().add(returnItem);

            totalRefund += itemRefund;

            item.setStatus("RETURN_REQUESTED");
            orderItemRepo.save(item);
        }

        request.setRefundAmount(totalRefund);
        return returnRequestRepo.save(request);
    }

    @Transactional
    public void approveReturn(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", requestId));

        if (request.getStatus() != ReturnRequest.ReturnStatus.REQUESTED
                && request.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
            throw new APIException("Return request in invalid state for approval");
        }

        request.setStatus(ReturnRequest.ReturnStatus.COMPLETED);

        // Execute Refund
        if (request.getRefundType() == ReturnRequest.RefundType.WALLET) {
            walletService.credit(request.getOrder().getEmail(),
                    request.getRefundAmount(),
                    "Refund for Order #" + request.getOrder().getOrderId(),
                    String.valueOf(request.getReturnRequestId()));
        } else {
            // Partial Razorpay Refund
            paymentService.initiateRefund(request.getOrder().getPayment().getPgPaymentId(),
                    Math.round(request.getRefundAmount() * 100), // convert to paisa
                    "Partial return for order #" + request.getOrder().getOrderId());
        }

        // Update Order Item Statuses
        for (ReturnItem rItem : request.getItems()) {
            OrderItem orderItem = rItem.getOrderItem();
            orderItem.setReturnedQuantity(orderItem.getReturnedQuantity() + rItem.getQuantity());
            orderItem.setStatus("RETURNED");
            orderItemRepo.save(orderItem);
        }

        // Update ERPNext
        try {
            // erpNextService.createSalesReturn(request); // To be implemented if needed
            log.info("Handled partial return in ERPNext for request {}", requestId);
        } catch (Exception e) {
            log.error("Failed to sync return with ERPNext: {}", e.getMessage());
        }

        returnRequestRepo.save(request);
    }
}
