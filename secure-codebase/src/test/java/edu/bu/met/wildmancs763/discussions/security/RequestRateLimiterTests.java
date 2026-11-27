/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%

*/

package edu.bu.met.wildmancs763.discussions.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestRateLimiterTests {

    @Test
    void locksOutForTenSecondsAfterFiftyRequests() {
        MutableClock clock = new MutableClock();
        RequestRateLimiter limiter = new RequestRateLimiter(
                50, Duration.ofMinutes(1), Duration.ofSeconds(10), Duration.ZERO, clock);

        for (int request = 0; request < 50; request++) {
            assertTrue(limiter.check("alice", false).allowed());
        }
        RequestRateLimiter.Decision locked = limiter.check("alice", false);
        assertFalse(locked.allowed());
        assertTrue(locked.lockout());
        assertEquals(10_000, locked.retryAfterMillis());

        clock.advance(Duration.ofSeconds(9).plusMillis(999));
        assertFalse(limiter.check("alice", false).allowed());
        clock.advance(Duration.ofMillis(1));
        assertTrue(limiter.check("alice", false).allowed());
    }

    @Test
    void enforcesMinimumIntervalBetweenExpensiveRequests() {
        MutableClock clock = new MutableClock();
        RequestRateLimiter limiter = new RequestRateLimiter(
                50, Duration.ofMinutes(1), Duration.ofSeconds(10), Duration.ofMillis(250), clock);

        assertTrue(limiter.check("alice", true).allowed());
        RequestRateLimiter.Decision tooSoon = limiter.check("alice", true);
        assertFalse(tooSoon.allowed());
        assertFalse(tooSoon.lockout());
        assertEquals(250, tooSoon.retryAfterMillis());

        clock.advance(Duration.ofMillis(249));
        assertFalse(limiter.check("alice", true).allowed());
        clock.advance(Duration.ofMillis(1));
        assertTrue(limiter.check("alice", true).allowed());
    }

    private static final class MutableClock extends Clock {

        private final AtomicReference<Instant> current =
                new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }

        void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }
    }
}
