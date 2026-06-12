package com.tony.erp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Proveedor de JWT seguro: genera, valida y extrae datos del token.
 * La clave secreta y la expiración se leen desde application.properties.
 */
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    // -------------------------------------------------------------------------
    // API Pública
    // -------------------------------------------------------------------------

    public String generarToken(String username, String role) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String headerB64 = URL_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

        long expEpochSecs = (System.currentTimeMillis() + expirationMs) / 1000;
        String payloadJson = "{\"sub\":\"" + username + "\",\"role\":\"" + role + "\",\"exp\":" + expEpochSecs + "}";
        String payloadB64 = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String dataToSign = headerB64 + "." + payloadB64;
        String signatureB64 = buildHmacSignature(dataToSign, secretKey);

        return dataToSign + "." + signatureB64;
    }

    public boolean validarToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String dataToValidate = parts[0] + "." + parts[1];
            String expectedSig = buildHmacSignature(dataToValidate, secretKey);

            if (!expectedSig.equals(parts[2])) return false;

            String payloadJson = decodeBase64(parts[1]);
            long expEpochSecs = extractLongClaim(payloadJson, "exp");
            long nowEpochSecs = System.currentTimeMillis() / 1000;

            return nowEpochSecs < expEpochSecs;

        } catch (Exception e) {
            return false;
        }
    }

    public String obtenerUsuarioDesdeToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payloadJson = decodeBase64(parts[1]);
            return extractStringClaim(payloadJson, "sub");
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Métodos privados
    // -------------------------------------------------------------------------

    private String buildHmacSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular firma HMAC-SHA256", e);
        }
    }

    private String decodeBase64(String encoded) {
        return new String(URL_DECODER.decode(encoded), StandardCharsets.UTF_8);
    }

    private String extractStringClaim(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private long extractLongClaim(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0L;
        start += pattern.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Long.parseLong(json.substring(start, end).trim());
    }
}
