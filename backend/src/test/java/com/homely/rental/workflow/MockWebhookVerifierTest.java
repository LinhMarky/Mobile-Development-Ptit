package com.homely.rental.workflow;

import com.homely.rental.auth.security.AccountAccessException;
import com.homely.rental.payment.service.MockWebhookVerifier;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.assertj.core.api.Assertions.*;

class MockWebhookVerifierTest {
    @Test void acceptsExactSignedBytesAndRejectsMissingMalformedOrModifiedSignature() throws Exception {
        String secret="only-for-tests-0123456789-abcdefghijklmnop";
        byte[] body="{\"status\":\"SUCCEEDED\"}".getBytes(StandardCharsets.UTF_8);
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String signature=HexFormat.of().formatHex(mac.doFinal(body));
        var verifier=new MockWebhookVerifier(secret);
        verifier.verify(body,signature);
        assertThatThrownBy(()->verifier.verify(body,null)).isInstanceOf(AccountAccessException.class);
        assertThatThrownBy(()->verifier.verify(body,"invalid")).isInstanceOf(AccountAccessException.class);
        assertThatThrownBy(()->verifier.verify("{}".getBytes(StandardCharsets.UTF_8),signature)).isInstanceOf(AccountAccessException.class);
        assertThatThrownBy(()->new MockWebhookVerifier("").verify(body,signature)).isInstanceOf(AccountAccessException.class);
    }
}
