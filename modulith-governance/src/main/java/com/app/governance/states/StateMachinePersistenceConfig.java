package com.app.governance.states;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;


@Configuration
public class StateMachinePersistenceConfig {

    @Bean
    public StateMachineRuntimePersister<Object, Object, String> stateMachineRuntimePersister(
            JpaStateMachineRepository jpaStateMachineRepository) {
        return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
    }

    @Bean
    public org.springframework.statemachine.persist.StateMachinePersister<Object, Object, String> stateMachinePersister(
            StateMachineRuntimePersister<Object, Object, String> persister) {
        return new org.springframework.statemachine.persist.DefaultStateMachinePersister<>(persister);
    }
}
