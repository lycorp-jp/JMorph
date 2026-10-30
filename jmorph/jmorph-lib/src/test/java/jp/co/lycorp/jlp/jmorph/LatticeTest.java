// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LatticeTest {

    @Test
    public void testGet() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        NodeImpl expected = new NodeImpl(-1, 1); // bos

        // exec
        NodeImpl actual = target.get(0);

        // verify
        assertEquals(expected.getLoc(), actual.getLoc());
        assertEquals(expected.getLength(), actual.getLength());
    }

    @Test
    public void testConnect() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        doReturn(1).when(mockCostMatrix).getCost(anyInt(), anyInt());
        Lattice target = spy(new Lattice(mockCostMatrix, sentence));

        NodeImpl rnode0 = spy(new NodeImpl(3, 4));
        NodeImpl rnode1 = spy(new NodeImpl(3, 5));
        List<NodeImpl> rnodeList = List.of(rnode0, rnode1);
        NodeImpl lnode = spy(new NodeImpl(0, 3));
        doReturn(lnode).when(target).get(anyInt());
        doReturn(10).when(lnode).getPathCost();
        doReturn(null).when(lnode).getNext();

        // exec
        target.connect(rnodeList);
        reset(target);

        // verify
        assertEquals(-1, target.get(0).loc);
        assertEquals(1, target.get(0).length);
        assertEquals(3, target.get(7).loc);
        assertEquals(4, target.get(7).length);
        assertEquals(3, target.get(8).loc);
        assertEquals(5, target.get(8).length);
    }

    @Test
    public void testConnectChecksTimeoutInsideLoop() {
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, new byte[1]);
        List<NodeImpl> nodes = new ArrayList<>();
        for (int i = 0; i < 1024; i++) {
            nodes.add(new NodeImpl(0, 1));
        }
        AtomicLong nanoTime = new AtomicLong(0);
        TimeoutGuard timeout = TimeoutGuard.start(1, nanoTime::get);
        nanoTime.set(1_000_000L);

        assertThrows(TimeoutExceededException.class, () -> target.connect(nodes, timeout));
    }

    @Test
    public void testFinish() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = spy(new Lattice(mockCostMatrix, sentence));

        doNothing().when(target).connect(anyList());

        // exec
        target.finish();

        // verify
        verify(target, times(1)).connect(anyList());
    }

    @Test
    public void testGetSentence() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        // exec
        byte[] actual = target.getSentence();

        // verify
        assertEquals(sentence, actual);
    }

    @Test
    public void testGetEos() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        // exec
        NodeImpl actual = target.getEos();

        // verify
        assertNull(actual);
    }

    @Test
    public void testGetBestPath0() throws SecurityException, IllegalArgumentException {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        List<NodeImpl> nodeList = Collections.singletonList(new NodeImpl(0, sentence.length));
        target.connect(nodeList);
        target.finish();

        List<NodeImpl> expected = Collections.emptyList();

        // exec
        List<NodeImpl> actual = target.getBestPath(0);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetBestPath1() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        List<NodeImpl> nodeList = Collections.singletonList(new NodeImpl(0, sentence.length));
        target.connect(nodeList);
        target.finish();

        // exec
        List<NodeImpl> actual = target.getBestPath(1);

        // verify
        assertEquals(nodeList, actual);
    }

    @Test
    public void testGetBestPath2() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        List<NodeImpl> nodeList = Collections.singletonList(new NodeImpl(0, sentence.length));
        target.connect(nodeList);
        target.finish();

        // exec
        List<NodeImpl> actual = target.getBestPath(2);

        // verify
        assertEquals(nodeList, actual);
    }

    @Test
    public void testGetNextPath0() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        Field field = target.getClass().getDeclaredField("nbestCount");
        field.setAccessible(true);
        field.set(target, 0);
        List<NodeImpl> expected = Collections.emptyList();

        // exec
        List<NodeImpl> actual = target.getNextPath();

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetNextPath1Empty() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        List<NodeImpl> nodeList = Collections.singletonList(new NodeImpl(0, sentence.length));
        target.connect(nodeList);
        target.finish();
        target.getBestPath(2);

        List<NodeImpl> expected = Collections.emptyList();

        // exec
        List<NodeImpl> actual = target.getNextPath();

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetNextPath1NotEmptyBos() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        Field nbestCountField = target.getClass().getDeclaredField("nbestCount");
        nbestCountField.setAccessible(true);
        nbestCountField.set(target, 1);

        Field nbestQueueField = target.getClass().getDeclaredField("nbestQueue");
        nbestQueueField.setAccessible(true);
        NbestQueue mockNbestQueue = spy(new NbestQueue());
        NodeImpl mockEos = spy(new NodeImpl(sentence.length, 1));
        NodeImpl mockExpectedNode = spy(new NodeImpl(0, sentence.length));
        Anode mockEosAnode = spy(new Anode(null, mockEos, 0));
        Anode mockNodeAnode = spy(new Anode(mockEosAnode, mockExpectedNode, 0));
        Anode mockBosAnode = spy(new Anode(mockNodeAnode, target.get(0), 0));
        mockNbestQueue.add(mockBosAnode);
        nbestQueueField.set(target, mockNbestQueue);

        // exec
        List<NodeImpl> actual = target.getNextPath();

        // verify
        assertEquals(1, actual.size());
        assertEquals(0, actual.get(0).loc);
        assertEquals(sentence.length, actual.get(0).length);
    }

    @Test
    public void testPush() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);
        CostMatrix mockCostMatrix = mock(CostMatrix.class);
        Lattice target = new Lattice(mockCostMatrix, sentence);

        NodeImpl node = new NodeImpl(3, 4);
        Method method = Lattice.class.getDeclaredMethod("push", NodeImpl.class);
        method.setAccessible(true);

        // exec
        method.invoke(target, node);

        // verify
        assertEquals(-1, target.get(0).loc);
        assertEquals(1, target.get(0).length);
        assertEquals(3, target.get(7).loc);
        assertEquals(4, target.get(7).length);
    }
}
