package com.homely.rental.payment.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.homely.rental.auth.security.AccountAccessException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class MockWebhookVerifier {
    private final String secret;
    public MockWebhookVerifier(@Value("${homely.payment.mock-webhook-secret:}") String secret) { this.secret = secret; }
    public void verify(byte[] body, String signature) {
        if (secret.length() < 32 || signature == null) throw new AccountAccessException(401,"INVALID_WEBHOOK_SIGNATURE","Webhook authentication failed");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature))) {
                throw new AccountAccessException(401,"INVALID_WEBHOOK_SIGNATURE","Webhook authentication failed");
            }
        } catch (IllegalArgumentException e) {
            throw new AccountAccessException(401,"INVALID_WEBHOOK_SIGNATURE","Webhook authentication failed");
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
}

