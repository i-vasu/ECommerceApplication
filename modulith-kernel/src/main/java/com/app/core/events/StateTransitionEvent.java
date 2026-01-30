package com.app.core.events;

import lombok.Value;

@Value
public class StateTransitionEvent {
    String entityType;
    Long entityId;
    Object sourceState;
    Object targetState;
    Object event;
    boolean success;
}
