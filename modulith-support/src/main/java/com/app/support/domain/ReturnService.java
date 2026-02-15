package com.app.support.domain;

import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.core.events.ReturnApprovedEvent;
import com.app.core.events.ReturnRequestedEvent;
import com.app.core.events.ReturnValidatedEvent;
import com.app.support.entities.ReturnItem;
import com.app.support.entities.ReturnRequest;
import com.app.support.repositories.ReturnRequestRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReturnService.class);

    private final ReturnRequestRepo returnRequestRepo;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Entry point for a user to request a return.
     * Decoupled: Emits an event for the Order module to validate.
     */
    @Transactional
    public void requestPartialReturn(Long orderId, String email, Map<Long, Integer> itemsToReturn,
            String reason, ReturnRequest.RefundType refundType) {

        log.info("Support: User {} requesting return for order {}", email, orderId);

        // Publish event for validation in Order module
        eventPublisher.publishEvent(new ReturnRequestedEvent(
                orderId,
                email,
                itemsToReturn,
                reason,
                refundType.name()));
    }

    /**
     * Listener for validated returns from the Order module.
     * Creates the return record in the Support database.
     */
    @EventListener
    @Transactional
    public void onReturnValidated(ReturnValidatedEvent event) {
        log.info("Support: Persisting validated return for order {}", event.orderId());

        ReturnRequest request = new ReturnRequest();
        request.setOrderId(event.orderId());
        request.setUserEmail(event.email());
        request.setReason(event.reason());
        request.setStatus(ReturnRequest.ReturnStatus.REQUESTED);
        request.setRefundType(ReturnRequest.RefundType.valueOf(event.refundType()));
        request.setRefundAmount(event.totalRefundAmount());

        List<ReturnItem> items = event.items().stream()
                .map(vItem -> {
                    ReturnItem item = new ReturnItem();
                    item.setReturnRequest(request);
                    item.setOrderItemId(vItem.orderItemId());
                    item.setItemCode(vItem.itemCode());
                    item.setQuantity(vItem.quantity());
                    item.setUnitRefundAmount(vItem.unitRefundAmount());
                    return item;
                }).collect(Collectors.toList());

        request.setItems(items);
        returnRequestRepo.save(request);
        log.info("Support: Return record #{} created for order {}", request.getReturnRequestId(), event.orderId());
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getUserReturns(String email) {
        return returnRequestRepo.findByUserEmail(email);
    }

    /**
     * Admin approves a return request. 
     * Status moves from REQUESTED to APPROVED.
     * This signals logistics to schedule a reverse pickup.
     */
    @Transactional
    public void approveReturn(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", requestId));

        if (request.getStatus() != ReturnRequest.ReturnStatus.REQUESTED) {
            throw new APIException("Return request must be in REQUESTED state to approve");
        }

        request.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        returnRequestRepo.save(request);

        log.info("Support: Return #{} approved. Ready for reverse pickup.", requestId);
        
        // Publish event to trigger Reverse Logistics (Shiprocket)
        eventPublisher.publishEvent(new com.app.core.events.ReturnPickupInitiatedEvent(
                requestId,
                request.getOrderId(),
                request.getUserEmail(),
                request.getReason(),
                request.getItems().stream()
                        .map(i -> new com.app.core.events.ReturnPickupInitiatedEvent.ApprovedReturnItem(i.getOrderItemId(), i.getItemCode(), i.getQuantity()))
                        .toList()));
    }

    /**
     * Admin/Operator marks return as received and inspected.
     * Status moves from APPROVED to COMPLETED.
     * Triggers the ReturnApprovedEvent which handles Refund and Restocking.
     */
    @Transactional
    public void markAsReceived(Long requestId, String adminComments) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", requestId));

        if (request.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
            throw new APIException("Return request must be APPROVED before it can be marked as RECEIVED");
        }

        request.setStatus(ReturnRequest.ReturnStatus.COMPLETED);
        request.setAdminComments(adminComments);
        returnRequestRepo.save(request);

        log.info("Support: Return #{} received and completed. Triggering refund/restock.", requestId);

        // Publish event for Order/Finance/ERP modules to execute the refund and restock logic
        eventPublisher.publishEvent(new ReturnApprovedEvent(
                requestId,
                request.getOrderId(),
                request.getUserEmail(),
                request.getRefundAmount(),
                request.getRefundType().name(),
                request.getItems().stream()
                        .map(i -> new ReturnApprovedEvent.ApprovedReturnItem(i.getOrderItemId(), i.getItemCode(), i.getQuantity()))
                        .toList()));
    }

    @Transactional
    public void rejectReturn(Long requestId, String reason) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", requestId));

        request.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        request.setAdminComments(reason);
        returnRequestRepo.save(request);
        log.info("Support: Return #{} rejected with reason: {}", requestId, reason);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReturnRequest> getAllReturns(int page, int size) {
        return returnRequestRepo.findAll(org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public boolean hasOpenTicketForOrder(Long orderId) {
        return returnRequestRepo.existsByOrderIdAndStatusNot(orderId, "CLOSED");
    }
}
