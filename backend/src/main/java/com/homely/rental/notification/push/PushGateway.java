package com.homely.rental.notification.push;
import java.util.Map;
public interface PushGateway {
    void send(String token, Map<String,String> data) throws PushFailure;
    class PushFailure extends Exception {
        public final String code;
        public final boolean retryable;
        public final boolean invalidToken;
        public PushFailure(String code, boolean retryable, boolean invalidToken) {
            super(code); this.code=code; this.retryable=retryable; this.invalidToken=invalidToken;
        }
    }
}
