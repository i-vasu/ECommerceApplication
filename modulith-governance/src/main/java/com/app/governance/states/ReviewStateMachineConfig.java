package com.app.governance.states;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "reviewStateMachineFactory")
public class ReviewStateMachineConfig extends EnumStateMachineConfigurerAdapter<ReviewState, ReviewEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<ReviewState, ReviewEvent> states) throws Exception {
        states.withStates().initial(ReviewState.PENDING).states(EnumSet.allOf(ReviewState.class));
    }
    @Override
    public void configure(StateMachineTransitionConfigurer<ReviewState, ReviewEvent> transitions) throws Exception {
        transitions
            .withExternal().source(ReviewState.PENDING).target(ReviewState.AUTO_APPROVED).event(ReviewEvent.APPROVE)
            .and()
            .withExternal().source(ReviewState.PENDING).target(ReviewState.MANUAL_REVIEW).event(ReviewEvent.FLAG)
            .and()
            .withExternal().source(ReviewState.AUTO_APPROVED).target(ReviewState.PUBLISHED).event(ReviewEvent.SUBMIT)
            .and()
            .withExternal().source(ReviewState.MANUAL_REVIEW).target(ReviewState.PUBLISHED).event(ReviewEvent.APPROVE)
            .and()
            .withExternal().source(ReviewState.PENDING).target(ReviewState.REJECTED).event(ReviewEvent.REJECT);
    }
}
