package com.homely.rental.auth.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/** Validates configuration without ever including the secret in an error message. */
public final class JwtSigningKey {
    private JwtSigningKey() { }

    public static SecretKey decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is required; supply a Base64-encoded random key of at least 64 bytes");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64");
        }
        if (bytes.length < 64) {
            throw new IllegalStateException("JWT_SECRET must contain at least 64 decoded bytes for HS512");
        }
        return new SecretKeySpec(bytes, "HmacSHA512");
    }
}
