package com.app.governance.states;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "walletStateMachineFactory")
public class WalletStateMachineConfig extends EnumStateMachineConfigurerAdapter<WalletState, WalletEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<WalletState, WalletEvent> states) throws Exception {
        states.withStates().initial(WalletState.PENDING).states(EnumSet.allOf(WalletState.class));
    }
    @Override
    public void configure(StateMachineTransitionConfigurer<WalletState, WalletEvent> transitions) throws Exception {
        transitions
            .withExternal().source(WalletState.PENDING).target(WalletState.COMPLETED).event(WalletEvent.CAPTURE)
            .and()
            .withExternal().source(WalletState.PENDING).target(WalletState.FAILED).event(WalletEvent.VOID)
            .and()
            .withExternal().source(WalletState.COMPLETED).target(WalletState.REVERSED).event(WalletEvent.REVERSE);
    }
}
