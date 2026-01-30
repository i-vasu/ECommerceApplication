package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "shipmentStateMachineFactory")
@Log4j2
public class ShipmentStateMachineConfig extends EnumStateMachineConfigurerAdapter<ShipmentState, ShipmentEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<ShipmentState, ShipmentEvent> states) throws Exception {
        states
            .withStates()
            .initial(ShipmentState.PENDING)
            .states(EnumSet.allOf(ShipmentState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ShipmentState, ShipmentEvent> transitions) throws Exception {
        transitions
            .withExternal().source(ShipmentState.PENDING).target(ShipmentState.READY_FOR_PICKUP).event(ShipmentEvent.ASSIGN_CARRIER)
            .and()
            .withExternal().source(ShipmentState.READY_FOR_PICKUP).target(ShipmentState.PICKED_UP).event(ShipmentEvent.PICKUP)
            .and()
            .withExternal().source(ShipmentState.PICKED_UP).target(ShipmentState.IN_TRANSIT).event(ShipmentEvent.SHIP)
            .and()
            .withExternal().source(ShipmentState.IN_TRANSIT).target(ShipmentState.DELIVERED).event(ShipmentEvent.ARRIVE_AT_DESTINATION)
            .and()
            .withExternal().source(ShipmentState.IN_TRANSIT).target(ShipmentState.DELIVERY_FAILED).event(ShipmentEvent.FAIL_DELIVERY)
            .and()
            .withExternal().source(ShipmentState.DELIVERY_FAILED).target(ShipmentState.RETURNED).event(ShipmentEvent.RETURN);
    }
}
