package com.homely.rental.auth.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

/** Email can be reused after deletion; an access token must also match the immutable account ID. */
public final class AccessTokenIdentity {
    public static final String USER_ID_CLAIM = "user_id";

    private AccessTokenIdentity() { }

    public static boolean matches(Jwt jwt, Long accountId) {
        if (accountId == null) return false;
        Object claimedId = jwt.getClaim(USER_ID_CLAIM);
        // Access tokens issued before v1.6 already contain user.id. Accept them until expiry.
        if (!jwt.getClaims().containsKey(USER_ID_CLAIM)) {
            Object user = jwt.getClaim("user");
            claimedId = user instanceof Map<?, ?> fields ? fields.get("id") : null;
        }
        if (!(claimedId instanceof Number)) return false;
        try {
            long id = Long.parseLong(claimedId.toString());
            return id > 0 && accountId.equals(id);
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
