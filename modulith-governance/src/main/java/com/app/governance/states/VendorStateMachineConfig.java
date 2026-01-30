package com.app.governance.states;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "vendorStateMachineFactory")
public class VendorStateMachineConfig extends EnumStateMachineConfigurerAdapter<VendorState, VendorEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<VendorState, VendorEvent> states) throws Exception {
        states.withStates().initial(VendorState.ONBOARDING).states(EnumSet.allOf(VendorState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<VendorState, VendorEvent> transitions) throws Exception {
        transitions
                .withExternal().source(VendorState.ONBOARDING).target(VendorState.ACTIVE).event(VendorEvent.APPROVE)
                .and()
                .withExternal().source(VendorState.ACTIVE).target(VendorState.SUSPENDED).event(VendorEvent.SUSPEND)
                .and()
                .withExternal().source(VendorState.SUSPENDED).target(VendorState.ACTIVE).event(VendorEvent.REACTIVATE)
                .and()
                .withExternal().source(VendorState.ACTIVE).target(VendorState.TERMINATED).event(VendorEvent.TERMINATE);
    }
}
