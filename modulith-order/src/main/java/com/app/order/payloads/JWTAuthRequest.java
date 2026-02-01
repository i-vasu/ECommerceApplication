package com.app.order.payloads;

public record JWTAuthRequest(String username, String password) {
}