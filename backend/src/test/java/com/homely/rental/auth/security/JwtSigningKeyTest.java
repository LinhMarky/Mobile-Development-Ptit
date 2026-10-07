package com.homely.rental.auth.security;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class JwtSigningKeyTest {
    @Test void rejectsMissingSecretAndShortKeys() {
        assertThatThrownBy(() -> JwtSigningKey.decode(null)).hasMessageContaining("JWT_SECRET is required");
        assertThatThrownBy(() -> JwtSigningKey.decode(" ")).hasMessageContaining("JWT_SECRET is required");
        String shortKey = Base64.getEncoder().encodeToString(new byte[32]);
        assertThatThrownBy(() -> JwtSigningKey.decode(shortKey)).hasMessageContaining("64 decoded bytes");
    }

    @Test void malformedSecretIsNeverReflectedInTheError() {
        assertThatThrownBy(() -> JwtSigningKey.decode("sensitive-invalid-value!"))
                .hasMessage("JWT_SECRET must be valid Base64");
    }

    @Test void acceptsA64ByteHs512Key() {
        var key = JwtSigningKey.decode(Base64.getEncoder().encodeToString(new byte[64]));
        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA512");
        assertThat(key.getEncoded()).hasSize(64);
    }
}
