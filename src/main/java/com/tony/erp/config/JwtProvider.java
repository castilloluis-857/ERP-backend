package com.tony.erp.config;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Componente responsable de la creación, validación y lectura de tokens JWT.
 *
 * Implementado con criptografía nativa de Java (HMAC-SHA256) para evitar
 * dependencias externas como jjwt o nimbus-jose.
 *
 * Ciclo de vida del token:
 *   generarToken() → el usuario hace login correctamente
 *   validarToken() → en cada petición HTTP protegida (JwtFilter)
 *   obtenerUsuarioDesdeToken() → para identificar al usuario autenticado
 */
@Component
public class JwtProvider {

    /**
     * Clave secreta para firmar los tokens con HMAC-SHA256.
     * En producción, esta clave debe moverse a application.properties
     * o a una variable de entorno segura (nunca hardcodeada en el fuente).
     */
    private static final String SECRET_KEY = "miClaveSecretaERPSuperFuerteYSeguraParaProduccion123456789!";

    /** Duración del token: 8 horas expresadas en milisegundos. */
    private static final long TOKEN_EXPIRATION_MS = 8 * 60 * 60 * 1000L;

    // -------------------------------------------------------------------------
    // API Pública
    // -------------------------------------------------------------------------

    /**
     * Genera un token JWT firmado para el usuario y rol indicados.
     *
     * Estructura del token:  headerB64.payloadB64.firmaB64
     *
     * @param username Nombre del usuario autenticado.
     * @param role     Rol principal del usuario (p.ej. "ROLE_ADMIN").
     * @return Token JWT como String.
     */
    public String generarToken(String username, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        // 1. Header: algoritmo y tipo de token
        String headerJson  = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String headerB64   = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

        // 2. Payload: datos del usuario y tiempo de expiración (epoch en segundos)
        long expEpochSecs  = (System.currentTimeMillis() + TOKEN_EXPIRATION_MS) / 1000;
        String payloadJson = "{\"sub\":\"" + username + "\",\"role\":\"" + role + "\",\"exp\":" + expEpochSecs + "}";
        String payloadB64  = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // 3. Firma criptográfica sobre header + payload
        String dataToSign  = headerB64 + "." + payloadB64;
        String signatureB64 = buildHmacSignature(dataToSign, SECRET_KEY);

        return dataToSign + "." + signatureB64;
    }

    /**
     * Valida que el token sea íntegro (firma correcta) y no haya expirado.
     *
     * @param token Token JWT recibido en la cabecera Authorization.
     * @return {@code true} si el token es válido y vigente.
     */
    public boolean validarToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // Reconstruimos la firma esperada y la comparamos con la recibida
            String dataToValidate  = parts[0] + "." + parts[1];
            String expectedSig     = buildHmacSignature(dataToValidate, SECRET_KEY);
            if (!expectedSig.equals(parts[2])) {
                return false; // Firma adulterada o incorrecta
            }

            // Verificamos la fecha de expiración dentro del payload
            String payloadJson = decodeBase64(parts[1]);
            long expEpochSecs  = extractLongClaim(payloadJson, "exp");
            long nowEpochSecs  = System.currentTimeMillis() / 1000;

            return nowEpochSecs < expEpochSecs;

        } catch (Exception e) {
            return false; // Cualquier error de parseo → token inválido
        }
    }

    /**
     * Extrae el nombre de usuario (claim "sub") del payload del token.
     *
     * @param token Token JWT.
     * @return Nombre de usuario, o {@code null} si no se puede extraer.
     */
    public String obtenerUsuarioDesdeToken(String token) {
        try {
            String[] parts     = token.split("\\.");
            String payloadJson = decodeBase64(parts[1]);
            return extractStringClaim(payloadJson, "sub");
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares privados
    // -------------------------------------------------------------------------

    /**
     * Calcula la firma HMAC-SHA256 de los datos con la clave secreta indicada.
     *
     * @param data   Cadena a firmar.
     * @param secret Clave secreta.
     * @return Firma en Base64 URL-safe sin padding.
     */
    private String buildHmacSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular firma HMAC-SHA256", e);
        }
    }

    /** Decodifica un segmento Base64 URL-safe a String UTF-8. */
    private String decodeBase64(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    /**
     * Extrae el valor de un claim de tipo String de un JSON simple.
     * Ejemplo: {"sub":"admin","role":"ROLE_ADMIN","exp":123} → extractStringClaim(json, "sub") = "admin"
     */
    private String extractStringClaim(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    /**
     * Extrae el valor de un claim de tipo numérico (long) de un JSON simple.
     * Ejemplo: {"exp":1718000000} → 1718000000L
     */
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
