package com.app.core.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HmacUtils {

    /**
     * Verifies an HMAC-SHA256 signature.
     */
    public static boolean verifyHmac(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // ERPNext often sends hex or base64.
            // Most modern integrations use Hex. Let's support both if possible or stick to
            // Hex.
            String calculatedHex = bytesToHex(hash);
            // Some integrations use Base64
            String calculatedBase64 = Base64.getEncoder().encodeToString(hash);

            return signature.equalsIgnoreCase(calculatedHex) || signature.equals(calculatedBase64);
        } catch (Exception e) {
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
