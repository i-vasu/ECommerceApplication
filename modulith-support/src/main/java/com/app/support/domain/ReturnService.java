package com.app.support.domain;

import com.app.support.entities.*;
import com.app.support.repositories.ReturnRequestRepo;
import com.app.core.events.ReturnRequestedEvent;
import com.app.core.events.ReturnValidatedEvent;
import com.app.core.events.ReturnApprovedEvent;
import com.app.core.events.ReturnValidatedEvent.ValidatedReturnItem;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;

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
@Log4j2
@RequiredArgsConstructor
public class ReturnService {

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
     * Admin approves a return after inspection.
     * Decoupled: Emits event for financial and ERP sync.
     */
    @Transactional
    public void approveReturn(Long requestId) {
        ReturnRequest request = returnRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", requestId));

        if (request.getStatus() != ReturnRequest.ReturnStatus.REQUESTED
                && request.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
            throw new APIException("Return request in invalid state for approval");
        }

        request.setStatus(ReturnRequest.ReturnStatus.COMPLETED);
        returnRequestRepo.save(request);

        log.info("Support: Return {} approved. Publishing event for fulfillment.", requestId);

        // Publish event for Order/Finance/ERP modules
        eventPublisher.publishEvent(new ReturnApprovedEvent(
                requestId,
                request.getOrderId(),
                request.getUserEmail(),
                request.getRefundAmount(),
                request.getRefundType().name(),
                request.getItems().stream()
                        .map(i -> new ReturnApprovedEvent.ApprovedReturnItem(i.getOrderItemId(), i.getQuantity()))
                        .toList()));
    }

    @Transactional(readOnly = true)
    public boolean hasOpenTicketForOrder(Long orderId) {
        return returnRequestRepo.existsByOrderIdAndStatusNot(orderId, "CLOSED");
    }
}
