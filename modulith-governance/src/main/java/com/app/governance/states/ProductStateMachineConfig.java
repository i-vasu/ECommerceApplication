package com.app.governance.states;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "productStateMachineFactory")
@Log4j2
public class ProductStateMachineConfig extends EnumStateMachineConfigurerAdapter<ProductState, ProductEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<ProductState, ProductEvent> states) throws Exception {
        states
            .withStates()
            .initial(ProductState.DRAFT)
            .states(EnumSet.allOf(ProductState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ProductState, ProductEvent> transitions) throws Exception {
        transitions
            .withExternal().source(ProductState.DRAFT).target(ProductState.ACTIVE).event(ProductEvent.PUBLISH)
            .and()
            .withExternal().source(ProductState.ACTIVE).target(ProductState.OUT_OF_STOCK).event(ProductEvent.SELL_OUT)
            .and()
            .withExternal().source(ProductState.OUT_OF_STOCK).target(ProductState.ACTIVE).event(ProductEvent.RESTOCK)
            .and()
            .withExternal().source(ProductState.ACTIVE).target(ProductState.DISCONTINUED).event(ProductEvent.DISCONTINUE)
            .and()
            .withExternal().source(ProductState.DISCONTINUED).target(ProductState.ARCHIVED).event(ProductEvent.ARCHIVE);
    }
}
