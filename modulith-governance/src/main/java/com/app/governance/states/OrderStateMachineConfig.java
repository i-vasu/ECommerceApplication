package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "orderStateMachineFactory")
@Log4j2
public class OrderStateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderState, OrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states
            .withStates()
            .initial(OrderState.PENDING)
            .states(EnumSet.allOf(OrderState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
            .withExternal().source(OrderState.PENDING).target(OrderState.PAYMENT_CAPTURED).event(OrderEvent.PAY)
            .and()
            .withExternal().source(OrderState.PENDING).target(OrderState.PAYMENT_FAILED).event(OrderEvent.PAY_FAIL)
            .and()
            .withExternal().source(OrderState.PAYMENT_CAPTURED).target(OrderState.PROCESSING).event(OrderEvent.APPROVE)
            .and()
            .withExternal().source(OrderState.PROCESSING).target(OrderState.SHIPPED).event(OrderEvent.SHIP)
            .and()
            .withExternal().source(OrderState.SHIPPED).target(OrderState.DELIVERED).event(OrderEvent.DELIVER)
            .and()
            .withExternal().source(OrderState.PENDING).target(OrderState.CANCELLED).event(OrderEvent.CANCEL)
            .and()
            .withExternal().source(OrderState.PAYMENT_CAPTURED).target(OrderState.CANCELLED).event(OrderEvent.CANCEL)
            .and()
            .withExternal().source(OrderState.PROCESSING).target(OrderState.CANCELLED).event(OrderEvent.CANCEL)
            .and()
            .withExternal().source(OrderState.PAYMENT_CAPTURED).target(OrderState.REFUNDED).event(OrderEvent.REFUND)
            .and()
            .withExternal().source(OrderState.DELIVERED).target(OrderState.RETURNED).event(OrderEvent.RETURN);
    }
}
