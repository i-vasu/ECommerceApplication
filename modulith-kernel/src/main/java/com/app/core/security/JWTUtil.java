package com.app.core.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Centralized JWT Utility for the Modulith platform.
 */
@Component
public class JWTUtil {

    @Value("${jwt_secret:VaabhiSecretKey2026}")
    private String secret;

    public record TokenData(String email, String tenantId) {
    }

    public String generateToken(String email, String tenantId) throws IllegalArgumentException, JWTCreationException {
        return JWT.create()
                .withSubject("User Details")
                .withClaim("email", email)
                .withClaim("tenantId", tenantId)
                .withIssuedAt(new Date())
                .withIssuer("Vaabhi Store")
                .sign(Algorithm.HMAC256(secret));
    }

    public TokenData decodeToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withSubject("User Details")
                .withIssuer("Vaabhi Store").build();

        DecodedJWT jwt = verifier.verify(token);
        return new TokenData(
                jwt.getClaim("email").asString(),
                jwt.getClaim("tenantId").asString());
    }
}
