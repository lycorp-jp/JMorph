// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class NbestQueueTest {

    NbestQueue target = new NbestQueue();

    @Test
    public void testIsEmpty() {
        // exec
        boolean actual = target.isEmpty();

        // verify
        assertTrue(actual);
    }

    @Test
    public void testPoll() {
        // setup
        NodeImpl nodeFirstAdd = new NodeImpl(10, 1);
        Anode anodeFirstAdd = new Anode(null, nodeFirstAdd, 0);
        NodeImpl nodeSecondAdd = new NodeImpl(8, 2);
        Anode anodeSecondAdd = new Anode(anodeFirstAdd, nodeSecondAdd, 0);
        target.add(anodeFirstAdd);
        target.add(anodeSecondAdd);

        // exec
        Anode actual1 = target.poll();
        Anode actual2 = target.poll();

        // verify
        assertTrue(target.isEmpty());
        assertEquals(anodeFirstAdd, actual1);
        assertEquals(anodeSecondAdd, actual2);
    }

    @Test
    public void testAdd() {
        // setup
        NodeImpl node = new NodeImpl(10, 1);
        Anode anode = new Anode(null, node, 0);

        // exec
        target.add(anode);

        // verify
        assertFalse(target.isEmpty());
        assertEquals(anode, target.poll());
        assertTrue(target.isEmpty());
    }

    @Test
    public void testAddChecksTimeoutInsideLinearSearch() {
        NodeImpl node = new NodeImpl(10, 1);
        Anode anode = new Anode(null, node, 0);
        for (int i = 0; i < 1024; i++) {
            target.add(anode);
        }
        AtomicLong nanoTime = new AtomicLong(0);
        TimeoutGuard timeout = TimeoutGuard.start(1, nanoTime::get);
        nanoTime.set(1_000_000L);

        assertThrows(TimeoutExceededException.class, () -> target.add(anode, timeout));
    }
}
