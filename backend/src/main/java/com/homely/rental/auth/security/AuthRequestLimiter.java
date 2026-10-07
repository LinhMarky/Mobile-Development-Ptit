package com.homely.rental.auth.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** A bounded, thread-safe fixed-window counter. Expired entries are reclaimed. */
@Component
public class AuthRequestLimiter {
    private final AuthRateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    private long nextCleanupAt;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthRequestLimiter(AuthRateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AuthRequestLimiter(AuthRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Returns zero when permitted, otherwise the number of seconds before retry. */
    public synchronized long acquire(String key, int maximum) {
        long now = clock.millis();
        long duration = properties.getWindowSeconds() * 1000L;
        if (now >= nextCleanupAt || windows.size() >= properties.getMaxEntries()) {
            windows.values().removeIf(window -> window.expiresAt <= now);
            nextCleanupAt = now + duration;
        }
        Window window = windows.get(key);
        if (window == null || window.expiresAt <= now) {
            if (window == null && windows.size() >= properties.getMaxEntries()) {
                return properties.getWindowSeconds();
            }
            window = new Window(now + duration);
            windows.put(key, window);
        }
        if (window.attempts >= maximum) {
            return Math.max(1, (window.expiresAt - now + 999) / 1000);
        }
        window.attempts++;
        return 0;
    }

    private static final class Window {
        private final long expiresAt;
        private int attempts;
        private Window(long expiresAt) { this.expiresAt = expiresAt; }
    }
}
