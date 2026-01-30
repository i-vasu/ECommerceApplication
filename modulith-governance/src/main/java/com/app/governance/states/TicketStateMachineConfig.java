package com.app.governance.states;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "ticketStateMachineFactory")
public class TicketStateMachineConfig extends EnumStateMachineConfigurerAdapter<TicketState, TicketEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<TicketState, TicketEvent> states) throws Exception {
        states.withStates().initial(TicketState.OPEN).states(EnumSet.allOf(TicketState.class));
    }
    @Override
    public void configure(StateMachineTransitionConfigurer<TicketState, TicketEvent> transitions) throws Exception {
        transitions
            .withExternal().source(TicketState.OPEN).target(TicketState.BOT_TRIAGE).event(TicketEvent.SUBMIT)
            .and()
            .withExternal().source(TicketState.BOT_TRIAGE).target(TicketState.AUTO_RESOLVED).event(TicketEvent.RESOLVE)
            .and()
            .withExternal().source(TicketState.BOT_TRIAGE).target(TicketState.ESCALATED).event(TicketEvent.ESCALATE)
            .and()
            .withExternal().source(TicketState.BOT_TRIAGE).target(TicketState.CLOSED).event(TicketEvent.CLOSE)
            .and()
            .withExternal().source(TicketState.AUTO_RESOLVED).target(TicketState.CLOSED).event(TicketEvent.CLOSE)
            .and()
            .withExternal().source(TicketState.ESCALATED).target(TicketState.CLOSED).event(TicketEvent.CLOSE);
    }
}
