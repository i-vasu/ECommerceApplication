package com.app.governance.states;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "pimStateMachineFactory")
public class PimStateMachineConfig extends EnumStateMachineConfigurerAdapter<PimState, PimEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<PimState, PimEvent> states) throws Exception {
        states.withStates().initial(PimState.DRAFT).states(EnumSet.allOf(PimState.class));
    }
    @Override
    public void configure(StateMachineTransitionConfigurer<PimState, PimEvent> transitions) throws Exception {
        transitions
            .withExternal().source(PimState.DRAFT).target(PimState.AI_TAGGING).event(PimEvent.SUBMIT)
            .and()
            .withExternal().source(PimState.AI_TAGGING).target(PimState.ENRICHED).event(PimEvent.TAG_COMPLETE)
            .and()
            .withExternal().source(PimState.ENRICHED).target(PimState.TRANSLATION_PENDING).event(PimEvent.ENRICH_COMPLETE)
            .and()
            .withExternal().source(PimState.TRANSLATION_PENDING).target(PimState.LIVE).event(PimEvent.TRANSLATE_COMPLETE)
            .and()
            .withExternal().source(PimState.ENRICHED).target(PimState.LIVE).event(PimEvent.PUBLISH);
    }
}
