// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class NodeTest {

    @Test
    public void testBos() {
        // setup
        NodeImpl bos = new NodeImpl(-1, 1);
        int expectedDictId = -1;
        int expectedLoc = -1;
        int expectedLength = 1;

        // exec
        int actualDictId = bos.getDictId();
        int actualLoc = bos.getLoc();
        int actualLength = bos.getLength();

        // verify
        assertEquals(expectedDictId, actualDictId);
        assertEquals(expectedLoc, actualLoc);
        assertEquals(expectedLength, actualLength);
    }

    @Test
    public void testEos() {
        // setup
        byte[] sentence = "あいうえお".getBytes(StandardCharsets.UTF_8);

        NodeImpl eos = new NodeImpl(sentence.length, 1);
        int expectedDictId = -1;
        int expectedLoc = sentence.length;
        int expectedLength = 1;

        // exec
        int actualDictId = eos.getDictId();
        int actualLoc = eos.getLoc();
        int actualLength = eos.getLength();

        // verify
        assertEquals(expectedDictId, actualDictId);
        assertEquals(expectedLoc, actualLoc);
        assertEquals(expectedLength, actualLength);
    }

    @Test
    public void testNodeImpl() {
        // setup
        TokenMecab token = new TokenMecab((short) 0, (short) 0, (short) 255, 123);
        int expectedDictId = 120;
        int expectedLoc = 8;
        int expectedLength = 3;
        int defaultPathCost = 0;
        NodeImpl defaultLeftLink = null;
        NodeImpl defaultNext = null;

        // exec
        NodeImpl node = new NodeImpl(token, 120, 8, 3);
        Token actualToken = node.getToken();
        int actualDictId = node.getDictId();
        int actualLoc = node.getLoc();
        int actualLength = node.getLength();
        int actualPathCost = node.getPathCost();
        NodeImpl actualLeftLink = node.getLeftLink();
        NodeImpl actualNext = node.getNext();

        // verify
        assertEquals(token, actualToken);
        assertEquals(expectedDictId, actualDictId);
        assertEquals(expectedLoc, actualLoc);
        assertEquals(expectedLength, actualLength);
        assertEquals(defaultPathCost, actualPathCost);
        assertEquals(defaultLeftLink, actualLeftLink);
        assertEquals(defaultNext, actualNext);
    }
}
