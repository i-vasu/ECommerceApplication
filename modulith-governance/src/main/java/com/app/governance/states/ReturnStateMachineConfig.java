package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "returnStateMachineFactory")
@Log4j2
public class ReturnStateMachineConfig extends EnumStateMachineConfigurerAdapter<ReturnState, ReturnEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<ReturnState, ReturnEvent> states) throws Exception {
        states
            .withStates()
            .initial(ReturnState.REQUESTED)
            .states(EnumSet.allOf(ReturnState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ReturnState, ReturnEvent> transitions) throws Exception {
        transitions
            .withExternal().source(ReturnState.REQUESTED).target(ReturnState.APPROVED).event(ReturnEvent.APPROVE)
            .and()
            .withExternal().source(ReturnState.APPROVED).target(ReturnState.PICKUP_SCHEDULED).event(ReturnEvent.SCHEDULE_PICKUP)
            .and()
            .withExternal().source(ReturnState.PICKUP_SCHEDULED).target(ReturnState.INSPECTION_PENDING).event(ReturnEvent.RECEIVE_AT_WAREHOUSE)
            .and()
            .withExternal().source(ReturnState.INSPECTION_PENDING).target(ReturnState.COMPLETED).event(ReturnEvent.INSPECT_SUCCESS)
            .and()
            .withExternal().source(ReturnState.INSPECTION_PENDING).target(ReturnState.REJECTED).event(ReturnEvent.INSPECT_FAIL)
            .and()
            .withExternal().source(ReturnState.REQUESTED).target(ReturnState.REJECTED).event(ReturnEvent.REJECT);
    }
}
