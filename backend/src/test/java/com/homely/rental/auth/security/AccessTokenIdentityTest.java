package com.homely.rental.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenIdentityTest {
    @Test void accountIdMustMatchEvenWhenEmailMatches() {
        Jwt jwt = token().claim("user_id", 7L).build();
        assertThat(AccessTokenIdentity.matches(jwt, 7L)).isTrue();
        assertThat(AccessTokenIdentity.matches(jwt, 8L)).isFalse();
    }

    @Test void legacyNestedUserIdRemainsSupported() {
        Jwt jwt = token().claim("user", Map.of("id", 7)).build();
        assertThat(AccessTokenIdentity.matches(jwt, 7L)).isTrue();
        assertThat(AccessTokenIdentity.matches(jwt, 8L)).isFalse();
    }

    @Test void missingMalformedOrConflictingIdIsRejected() {
        assertThat(AccessTokenIdentity.matches(token().build(), 7L)).isFalse();
        for (Object invalid : new Object[]{"7", -7, 7.5}) {
            Jwt jwt = token().claim("user_id", invalid).claim("user", Map.of("id", 7L)).build();
            assertThat(AccessTokenIdentity.matches(jwt, 7L)).isFalse();
        }
        assertThat(AccessTokenIdentity.matches(token().claim("user_id", 8L).claim("user", Map.of("id", 7L)).build(), 7L)).isFalse();
    }

    private Jwt.Builder token() {
        return Jwt.withTokenValue("access").header("alg", "HS512").subject("reused@example.test");
    }
}
