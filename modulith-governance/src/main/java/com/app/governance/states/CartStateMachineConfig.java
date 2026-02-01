package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "cartStateMachineFactory")
@Log4j2
public class CartStateMachineConfig extends EnumStateMachineConfigurerAdapter<CartState, CartEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<CartState, CartEvent> states) throws Exception {
        states
                .withStates()
                .initial(CartState.ACTIVE)
                .states(EnumSet.allOf(CartState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<CartState, CartEvent> transitions) throws Exception {
        transitions
                .withExternal().source(CartState.ACTIVE).target(CartState.ABANDONED).event(CartEvent.ABANDON)
                .and()
                .withExternal().source(CartState.ABANDONED).target(CartState.RECOVERED).event(CartEvent.RECOVER)
                .and()
                .withExternal().source(CartState.RECOVERED).target(CartState.CONVERTED).event(CartEvent.CONVERT)
                .and()
                .withExternal().source(CartState.ACTIVE).target(CartState.CONVERTED).event(CartEvent.CONVERT);
    }
}
