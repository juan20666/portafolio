package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts requests per key in fixed time windows. It lives in memory, which is enough
 * for a single instance; with several instances a shared store (Redis...) would be needed.
 *
 * <p>Memory is bounded: when too many keys are tracked, the expired ones are dropped,
 * and if that is not enough the whole table is reset.
 */
public final class FixedWindowRateLimiter {

    /** Outcome of one attempt. {@code retryAfterSeconds} is 0 when the request is allowed. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private record Window(long startedAtMillis, int count) {
    }

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequests, Duration window) {
        if (maxRequests <= 0 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("The limit and the window must be positive");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
    }

    public Decision tryAcquire(String key, long nowMillis) {
        if (windows.size() >= MAX_TRACKED_KEYS) {
            dropExpired(nowMillis);
            if (windows.size() >= MAX_TRACKED_KEYS) {
                windows.clear();
            }
        }
        Window current = windows.merge(
                key,
                new Window(nowMillis, 1),
                (previous, fresh) -> nowMillis - previous.startedAtMillis() >= windowMillis
                        ? fresh
                        : new Window(previous.startedAtMillis(), Math.min(previous.count() + 1, maxRequests + 1)));

        if (current.count() <= maxRequests) {
            return new Decision(true, 0);
        }
        long remainingMillis = current.startedAtMillis() + windowMillis - nowMillis;
        return new Decision(false, Math.max(1, (remainingMillis + 999) / 1000));
    }

    private void dropExpired(long nowMillis) {
        windows.entrySet().removeIf(entry -> nowMillis - entry.getValue().startedAtMillis() >= windowMillis);
    }
}
