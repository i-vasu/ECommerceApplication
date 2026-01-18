package com.app.order.payloads;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;

public record LoginCredentials(
		@Email @Column(unique = true, nullable = false) String email,
		String password) {
}
