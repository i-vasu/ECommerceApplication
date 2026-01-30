package com.app.order.payloads;

import com.app.security.payloads.UserDTO;

public record JWTAuthResponse(String token, UserDTO user) {
}