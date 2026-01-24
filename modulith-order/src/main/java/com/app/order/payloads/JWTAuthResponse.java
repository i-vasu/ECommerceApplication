package com.app.order.payloads;

import com.app.identity.payloads.UserDTO;

public record JWTAuthResponse(String token, UserDTO user) {
}