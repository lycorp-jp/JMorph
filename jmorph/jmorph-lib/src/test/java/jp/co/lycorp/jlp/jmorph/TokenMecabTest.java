// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TokenMecabTest {

    @Test
    public void testTokenMecab() {
        // exec
        TokenMecab actual = new TokenMecab();

        // verify
        assertEquals(0, actual.getLc());
        assertEquals(0, actual.getRc());
        assertEquals(10000, actual.getCost());
        assertEquals(-1, actual.getFeatureSetId());
        assertEquals(-1, actual.getTokenId());
    }

    @Test
    public void testTokenMecabArgs() {
        // exec
        TokenMecab actual = new TokenMecab((short) 1, (short) 2, (short) 300, 400);

        // verify
        assertEquals(1, actual.getLc());
        assertEquals(2, actual.getRc());
        assertEquals(300, actual.getCost());
        assertEquals(400, actual.getFeatureSetId());
        assertEquals(-1, actual.getTokenId());
    }

    @Test
    public void testTokenMecabUserDic() {
        // exec
        TokenMecab actual = new TokenMecab((short) 1, (short) 2, (short) 300, 400, 500);

        // verify
        assertEquals(1, actual.getLc());
        assertEquals(2, actual.getRc());
        assertEquals(300, actual.getCost());
        assertEquals(400, actual.getFeatureSetId());
        assertEquals(500, actual.getTokenId());
    }

    @Test
    public void testToBytes() {
        // setup
        TokenMecab target = new TokenMecab((short) 0x0102, (short) 0x0304, (short) 0x0506, 0x0708090A);

        // exec
        byte[] actual = target.toBytes();

        // verify
        assertEquals(TokenMecab.NUM_BYTES, actual.length);
        assertEquals((byte) 0x02, actual[0]);
        assertEquals((byte) 0x01, actual[1]);
        assertEquals((byte) 0x04, actual[2]);
        assertEquals((byte) 0x03, actual[3]);
        assertEquals((byte) 0x06, actual[4]);
        assertEquals((byte) 0x05, actual[5]);
        assertEquals((byte) 0x0A, actual[6]);
        assertEquals((byte) 0x09, actual[7]);
        assertEquals((byte) 0x08, actual[8]);
        assertEquals((byte) 0x07, actual[9]);
    }
}
