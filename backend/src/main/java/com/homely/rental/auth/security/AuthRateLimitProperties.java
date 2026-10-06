package com.homely.rental.auth.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Limits are intentionally local to one backend instance, like the chat limiter. */
@ConfigurationProperties(prefix = "homely.auth.rate-limit")
@Validated
@Getter
@Setter
public class AuthRateLimitProperties {
    private boolean enabled = true;
    @Min(1) private int windowSeconds = 60;
    @Min(1) private int maxEntries = 10_000;
    @Valid private Limit login = new Limit(60, 10);
    @Valid private Limit register = new Limit(20, 3);
    @Valid private Limit refresh = new Limit(120, 30);
    @Valid private Limit verifyEmail = new Limit(60, 10);
    @Valid private Limit resend = new Limit(20, 1);

    @Getter
    @Setter
    public static class Limit {
        @Min(1) private int perIp;
        @Min(1) private int perIdentity;

        public Limit() { }
        public Limit(int perIp, int perIdentity) {
            this.perIp = perIp;
            this.perIdentity = perIdentity;
        }
    }
}
