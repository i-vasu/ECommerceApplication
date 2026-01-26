package com.app.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when user is successfully registered.
 * Consumed by Order module to create initial cart.
 */
public record UserRegisteredEvent(
        @JsonProperty("userId") Long userId,
        @JsonProperty("email") String email,
        @JsonProperty("firstName") String firstName,
        @JsonProperty("lastName") String lastName) {
}
