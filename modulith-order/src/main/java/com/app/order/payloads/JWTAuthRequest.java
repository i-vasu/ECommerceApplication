package com.app.order.payloads;

import lombok.Data;

public record JWTAuthRequest(String username, String password) {
}