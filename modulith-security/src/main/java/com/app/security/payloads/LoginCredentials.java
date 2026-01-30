package com.app.security.payloads;

import jakarta.validation.constraints.NotBlank;

public record LoginCredentials(
    @NotBlank String email,
    @NotBlank String password
) {}
