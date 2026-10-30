// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AnodeTest {
    @Test
    public void testConstructor() {
        // setup
        NodeImpl parentNode = new NodeImpl(5, 4);
        NodeImpl mnode = new NodeImpl(2, 3);
        Anode parentAnode = new Anode(null, parentNode, 0);

        // exec
        Anode actual = new Anode(parentAnode, mnode, 0);

        // verify
        assertEquals(parentAnode, actual.parent);
        assertEquals(mnode, actual.mnode);
        assertEquals(0, actual.costG);
    }
}
