package com.homely.rental.auth.security;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class AuthRequestLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final AuthRateLimitProperties properties = new AuthRateLimitProperties();
    private final AuthRequestLimiter limiter = new AuthRequestLimiter(properties, clock);

    @Test void resetsOnlyAfterTheWindowExpiresAndReturnsRemainingSeconds() {
        assertThat(limiter.acquire("user", 1)).isZero();
        clock.now = clock.now.plusSeconds(12);
        assertThat(limiter.acquire("user", 1)).isEqualTo(48);
        clock.now = clock.now.plusSeconds(48);
        assertThat(limiter.acquire("user", 1)).isZero();
    }

    @Test void fullCounterMapDoesNotAllowUnlimitedNewIdentities() {
        properties.setMaxEntries(1);
        assertThat(limiter.acquire("first", 2)).isZero();
        assertThat(limiter.acquire("second", 2)).isEqualTo(60);
        clock.now = clock.now.plusSeconds(60);
        assertThat(limiter.acquire("second", 2)).isZero();
    }

    @Test void simultaneousRequestsCannotExceedTheLimit() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            var requests = new java.util.ArrayList<Future<Long>>();
            for (int i = 0; i < 40; i++) requests.add(pool.submit(() -> limiter.acquire("same-account", 10)));
            int accepted = 0;
            for (var request : requests) if (request.get(5, TimeUnit.SECONDS) == 0) accepted++;
            assertThat(accepted).isEqualTo(10);
        } finally { pool.shutdownNow(); }
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
