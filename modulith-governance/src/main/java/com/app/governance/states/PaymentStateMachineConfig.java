package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "paymentStateMachineFactory")
@Log4j2
public class PaymentStateMachineConfig extends EnumStateMachineConfigurerAdapter<PaymentState, PaymentEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        states
            .withStates()
            .initial(PaymentState.PENDING)
            .states(EnumSet.allOf(PaymentState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        transitions
            .withExternal().source(PaymentState.PENDING).target(PaymentState.AUTHORIZED).event(PaymentEvent.AUTHORIZE)
            .and()
            .withExternal().source(PaymentState.AUTHORIZED).target(PaymentState.CAPTURED).event(PaymentEvent.CAPTURE)
            .and()
            .withExternal().source(PaymentState.PENDING).target(PaymentState.FAILED).event(PaymentEvent.FAIL)
            .and()
            .withExternal().source(PaymentState.CAPTURED).target(PaymentState.REFUNDED).event(PaymentEvent.PROCESS_REFUND);
    }
}
