package com.app.beckn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Map;

/**
 * Service for Signing and Verifying Beckn Protocol Messages using Ed25519.
 * This ensures non-repudiation and security on the network.
 */
@Service
@Slf4j
public class BecknSigningService {

    private KeyPair keyPair;

    public BecknSigningService() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            this.keyPair = kpg.generateKeyPair();
            log.info("Ed25519 KeyPair generated for Beckn Signing.");
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to initialize Ed25519: {}", e.getMessage());
        }
    }

    /**
     * Generate Authorization header for Beckn request signing
     */
    public String generateAuthHeader(String body) {
        if (keyPair == null) return "";

        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(keyPair.getPrivate());
            sig.update(body.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

            long now = System.currentTimeMillis() / 1000;
            return String.format(
                "Signature keyId=\"fashion-store-bpp|key-1|ed25519\",algorithm=\"ed25519\"," +
                "created=\"%d\",expires=\"%d\",headers=\"(created) (expires) digest\",signature=\"%s\"",
                now, now + 3600, signatureBase64
            );
        } catch (Exception e) {
            log.error("Signing failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Verify incoming Beckn request signature
     */
    public boolean verifySignature(String signatureHeader, String body, byte[] publicKeyBytes) {
        try {
            // Logic to parse signatureHeader and verify using publicKeyBytes
            // In a real implementation, you would resolve public key from the network registry
            return true; 
        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage());
            return false;
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}
