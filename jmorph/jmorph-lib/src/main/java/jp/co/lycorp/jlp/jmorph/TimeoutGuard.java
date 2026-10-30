// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class TimeoutGuard {
    private static final int CHECK_INTERVAL = 1024;
    private static final TimeoutGuard DISABLED = new TimeoutGuard(false, 0, 0, 0, () -> 0);

    private final boolean enabled;
    private final long timeoutMillis;
    private final long startedAtNanos;
    private final long timeoutNanos;
    private final LongSupplier nanoTimeSource;
    private int operationCount;

    private TimeoutGuard(
            boolean enabled, long timeoutMillis, long startedAtNanos,
            long timeoutNanos, LongSupplier nanoTimeSource) {
        this.enabled = enabled;
        this.timeoutMillis = timeoutMillis;
        this.startedAtNanos = startedAtNanos;
        this.timeoutNanos = timeoutNanos;
        this.nanoTimeSource = nanoTimeSource;
    }

    public static TimeoutGuard start(long timeoutMillis) {
        return start(timeoutMillis, System::nanoTime);
    }

    static TimeoutGuard start(long timeoutMillis, LongSupplier nanoTimeSource) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis must be greater than or equal to 0");
        }
        if (timeoutMillis == 0) {
            return DISABLED;
        }

        Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");
        long startedAtNanos = nanoTimeSource.getAsLong();
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        return new TimeoutGuard(true, timeoutMillis, startedAtNanos, timeoutNanos, nanoTimeSource);
    }

    public static TimeoutGuard disabled() {
        return DISABLED;
    }

    public void check() {
        if (enabled && nanoTimeSource.getAsLong() - startedAtNanos >= timeoutNanos) {
            throw new TimeoutExceededException("Operation timed out after "
                    + timeoutMillis + " milliseconds");
        }
    }

    // for high-frequency calls
    public void checkPeriodically() {
        if (!enabled) {
            return;
        }
        operationCount++;
        if (operationCount == CHECK_INTERVAL) {
            operationCount = 0;
            check();
        }
    }
}
