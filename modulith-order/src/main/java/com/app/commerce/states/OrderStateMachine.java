package com.app.commerce.states;

import com.app.core.APIException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.PAYMENT_CAPTURED, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_CAPTURED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED), // Cannot CANCEL after ship
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUNDED)
    );

    public OrderStatus transition(OrderStatus current, OrderStatus next) {
        if (current == next) return current;

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        
        if (!allowed.contains(next)) {
            throw new APIException("Invalid State Transition: " + current + " -> " + next);
        }
        
        return next;
    }
    
    public boolean canCancel(OrderStatus current) {
        return current != OrderStatus.SHIPPED && 
               current != OrderStatus.DELIVERED && 
               current != OrderStatus.CANCELLED;
    }
}
