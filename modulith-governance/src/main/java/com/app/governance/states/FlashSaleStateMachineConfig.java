package com.app.governance.states;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "flashSaleStateMachineFactory")
public class FlashSaleStateMachineConfig extends EnumStateMachineConfigurerAdapter<FlashSaleState, FlashSaleEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<FlashSaleState, FlashSaleEvent> states) throws Exception {
        states.withStates().initial(FlashSaleState.SCHEDULED).states(EnumSet.allOf(FlashSaleState.class));
    }
    @Override
    public void configure(StateMachineTransitionConfigurer<FlashSaleState, FlashSaleEvent> transitions) throws Exception {
        transitions
            .withExternal().source(FlashSaleState.SCHEDULED).target(FlashSaleState.ACTIVE).event(FlashSaleEvent.START)
            .and()
            .withExternal().source(FlashSaleState.ACTIVE).target(FlashSaleState.ENDED).event(FlashSaleEvent.END)
            .and()
            .withExternal().source(FlashSaleState.SCHEDULED).target(FlashSaleState.CANCELLED).event(FlashSaleEvent.CANCEL)
            .and()
            .withExternal().source(FlashSaleState.ACTIVE).target(FlashSaleState.CANCELLED).event(FlashSaleEvent.CANCEL);
    }
}
