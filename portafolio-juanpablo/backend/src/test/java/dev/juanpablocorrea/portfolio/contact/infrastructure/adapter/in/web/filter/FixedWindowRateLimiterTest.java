package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedWindowRateLimiterTest {

    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, Duration.ofSeconds(60));

    @Test
    void allowsUpToTheLimitThenRejectsWithTheTimeLeft() {
        assertTrue(limiter.tryAcquire("a", 0).allowed());
        assertTrue(limiter.tryAcquire("a", 1_000).allowed());

        FixedWindowRateLimiter.Decision third = limiter.tryAcquire("a", 10_000);

        assertFalse(third.allowed());
        assertEquals(50, third.retryAfterSeconds());
    }

    @Test
    void eachKeyHasItsOwnCounter() {
        limiter.tryAcquire("a", 0);
        limiter.tryAcquire("a", 0);

        assertFalse(limiter.tryAcquire("a", 0).allowed());
        assertTrue(limiter.tryAcquire("b", 0).allowed());
    }

    @Test
    void theWindowResetsAfterItsLengthPasses() {
        limiter.tryAcquire("a", 0);
        limiter.tryAcquire("a", 0);
        assertFalse(limiter.tryAcquire("a", 59_999).allowed());

        assertTrue(limiter.tryAcquire("a", 60_000).allowed());
    }

    @Test
    void retryAfterIsRoundedUpAndNeverBelowOneSecond() {
        limiter.tryAcquire("a", 0);
        limiter.tryAcquire("a", 0);

        assertEquals(1, limiter.tryAcquire("a", 59_900).retryAfterSeconds());
    }

    @Test
    void refusesNonsensicalConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(1, Duration.ZERO));
    }
}
