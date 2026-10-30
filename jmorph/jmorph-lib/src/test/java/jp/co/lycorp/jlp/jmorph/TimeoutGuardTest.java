// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TimeoutGuardTest {

    @Test
    void testCheckDoesNotThrowBeforeTimeout() {
        AtomicLong nanoTime = new AtomicLong(1_000L);
        TimeoutGuard guard = TimeoutGuard.start(10, nanoTime::get);
        nanoTime.set(10_000_999L);

        assertDoesNotThrow(guard::check);
    }

    @Test
    void testCheckThrowsWhenTimeoutIsReached() {
        AtomicLong nanoTime = new AtomicLong(1_000L);
        TimeoutGuard guard = TimeoutGuard.start(10, nanoTime::get);
        nanoTime.set(10_001_000L);

        assertThrows(TimeoutExceededException.class, guard::check);
    }

    @Test
    void testCheckPeriodicallyChecksEvery1024Operations() {
        AtomicLong nanoTime = new AtomicLong(0);
        TimeoutGuard guard = TimeoutGuard.start(1, nanoTime::get);
        nanoTime.set(1_000_000L);

        for (int i = 0; i < 1023; i++) {
            guard.checkPeriodically();
        }
        assertThrows(TimeoutExceededException.class, guard::checkPeriodically);
    }

    @Test
    void testDisabledGuardNeverThrows() {
        assertDoesNotThrow(TimeoutGuard.disabled()::check);
        assertDoesNotThrow(TimeoutGuard.start(0)::check);
    }

    @Test
    void testNegativeTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> TimeoutGuard.start(-1));
    }
}
