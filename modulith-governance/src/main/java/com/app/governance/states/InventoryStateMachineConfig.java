package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "inventoryStateMachineFactory")
@Log4j2
public class InventoryStateMachineConfig extends EnumStateMachineConfigurerAdapter<InventoryState, InventoryEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<InventoryState, InventoryEvent> states) throws Exception {
        states
            .withStates()
            .initial(InventoryState.AVAILABLE)
            .states(EnumSet.allOf(InventoryState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<InventoryState, InventoryEvent> transitions) throws Exception {
        transitions
            .withExternal().source(InventoryState.AVAILABLE).target(InventoryState.RESERVED).event(InventoryEvent.RESERVE)
            .and()
            .withExternal().source(InventoryState.RESERVED).target(InventoryState.COMMITTED).event(InventoryEvent.COMMIT)
            .and()
            .withExternal().source(InventoryState.RESERVED).target(InventoryState.AVAILABLE).event(InventoryEvent.CANCEL_RESERVATION)
            .and()
            .withExternal().source(InventoryState.COMMITTED).target(InventoryState.RELEASED).event(InventoryEvent.RESTOCK);
    }
}
