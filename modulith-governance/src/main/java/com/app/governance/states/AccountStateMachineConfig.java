package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "accountStateMachineFactory")
@Log4j2
public class AccountStateMachineConfig extends EnumStateMachineConfigurerAdapter<AccountState, AccountEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<AccountState, AccountEvent> states) throws Exception {
        states
            .withStates()
            .initial(AccountState.PENDING_VERIFICATION)
            .states(EnumSet.allOf(AccountState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<AccountState, AccountEvent> transitions) throws Exception {
        transitions
            .withExternal().source(AccountState.PENDING_VERIFICATION).target(AccountState.ACTIVE).event(AccountEvent.VERIFY)
            .and()
            .withExternal().source(AccountState.ACTIVE).target(AccountState.SUSPENDED).event(AccountEvent.SUSPEND)
            .and()
            .withExternal().source(AccountState.SUSPENDED).target(AccountState.ACTIVE).event(AccountEvent.REACTIVATE)
            .and()
            .withExternal().source(AccountState.ACTIVE).target(AccountState.BANNED).event(AccountEvent.BAN)
            .and()
            .withExternal().source(AccountState.ACTIVE).target(AccountState.CLOSED).event(AccountEvent.CLOSE);
    }
}
