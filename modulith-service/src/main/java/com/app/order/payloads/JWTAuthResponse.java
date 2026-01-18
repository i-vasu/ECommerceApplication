package com.app.order.payloads;

import lombok.Data;

public record JWTAuthResponse(String token, UserDTO user) {
}