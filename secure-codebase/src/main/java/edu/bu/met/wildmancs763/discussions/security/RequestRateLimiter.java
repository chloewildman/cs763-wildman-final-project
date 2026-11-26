package edu.bu.met.wildmancs763.discussions.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestRateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maximumRequests;
    private final Duration windowDuration;
    private final Duration lockoutDuration;
    private final Duration expensiveMinimumInterval;
    private final AtomicLong cleanupCounter = new AtomicLong();

    @Autowired
    public RequestRateLimiter(
            @Value("${app.rate-limit.max-requests:50}") int maximumRequests,
            @Value("${app.rate-limit.window:60s}") Duration windowDuration,
            @Value("${app.rate-limit.lockout:10s}") Duration lockoutDuration,
            @Value("${app.rate-limit.expensive-min-interval:250ms}") Duration expensiveMinimumInterval) {
        this(maximumRequests, windowDuration, lockoutDuration, expensiveMinimumInterval, Clock.systemUTC());
    }

    RequestRateLimiter(
            int maximumRequests,
            Duration windowDuration,
            Duration lockoutDuration,
            Duration expensiveMinimumInterval,
            Clock clock) {
        if (maximumRequests <= 0 || windowDuration.isNegative() || windowDuration.isZero()
                || lockoutDuration.isNegative() || lockoutDuration.isZero()
                || expensiveMinimumInterval.isNegative()) {
            throw new IllegalArgumentException("Invalid rate-limit configuration.");
        }
        this.maximumRequests = maximumRequests;
        this.windowDuration = windowDuration;
        this.lockoutDuration = lockoutDuration;
        this.expensiveMinimumInterval = expensiveMinimumInterval;
        this.clock = clock;
    }

    public Decision check(String key, boolean expensive) {
        long now = clock.millis();
        Window window = windows.computeIfAbsent(key, ignored -> new Window());
        Decision decision;
        synchronized (window) {
            if (now < window.lockedUntil) {
                decision = new Decision(false, window.lockedUntil - now, true);
            } else {
                while (!window.requestTimes.isEmpty()
                        && now - window.requestTimes.peekFirst() >= windowDuration.toMillis()) {
                    window.requestTimes.removeFirst();
                }
                if (window.requestTimes.size() >= maximumRequests) {
                    window.lockedUntil = now + lockoutDuration.toMillis();
                    window.requestTimes.clear();
                    decision = new Decision(false, lockoutDuration.toMillis(), true);
                } else if (expensive && now < window.lastExpensiveAt + expensiveMinimumInterval.toMillis()) {
                    decision = new Decision(false,
                            window.lastExpensiveAt + expensiveMinimumInterval.toMillis() - now,
                            false);
                } else {
                    window.requestTimes.addLast(now);
                    if (expensive) {
                        window.lastExpensiveAt = now;
                    }
                    decision = new Decision(true, 0, false);
                }
            }
        }
        cleanStaleWindows(now);
        return decision;
    }

    private void cleanStaleWindows(long now) {
        if (cleanupCounter.incrementAndGet() % 1024 != 0) {
            return;
        }
        long staleAfter = windowDuration.plus(lockoutDuration).toMillis();
        windows.entrySet().removeIf(entry -> {
            Window window = entry.getValue();
            synchronized (window) {
                return now > window.lockedUntil
                        && (window.requestTimes.isEmpty()
                        || now - window.requestTimes.peekLast() > staleAfter);
            }
        });
    }

    public record Decision(boolean allowed, long retryAfterMillis, boolean lockout) {
    }

    private static final class Window {
        private final Deque<Long> requestTimes = new ArrayDeque<>();
        private long lockedUntil;
        private long lastExpensiveAt = Long.MIN_VALUE;
    }
}
