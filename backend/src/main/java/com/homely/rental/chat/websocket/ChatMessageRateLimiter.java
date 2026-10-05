package com.homely.rental.chat.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Process-local limit shared by every WebSocket session belonging to a user. */
@Component
public class ChatMessageRateLimiter {
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_USERS = 10_000;
    private final Map<String, Window> windows = new HashMap<>();
    private final int messagesPerMinute;
    private final Clock clock;
    private long nextCleanup;

    @Autowired
    public ChatMessageRateLimiter(@Value("${homely.chat.messages-per-minute:60}") int messagesPerMinute) {
        this(messagesPerMinute, Clock.systemUTC());
    }

    ChatMessageRateLimiter(int messagesPerMinute, Clock clock) {
        if (messagesPerMinute < 1) {
            throw new IllegalArgumentException("Chat message rate limit must be positive");
        }
        this.messagesPerMinute = messagesPerMinute;
        this.clock = clock;
    }

    synchronized void acquire(String email) {
        long now = clock.millis();
        if (now >= nextCleanup) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
            nextCleanup = now + WINDOW_MILLIS;
        }
        Window window = windows.get(email);
        if (window == null || now - window.startedAt >= WINDOW_MILLIS) {
            if (window == null && windows.size() >= MAX_TRACKED_USERS) {
                throw limited();
            }
            window = new Window(now);
            windows.put(email, window);
        }
        if (window.count >= messagesPerMinute) {
            throw limited();
        }
        window.count++;
    }

    private ChatProtocolException limited() {
        return new ChatProtocolException(429, "RATE_LIMITED", "Too many messages. Please wait before retrying.");
    }

    private static final class Window {
        final long startedAt;
        int count;
        Window(long startedAt) { this.startedAt = startedAt; }
    }
}
