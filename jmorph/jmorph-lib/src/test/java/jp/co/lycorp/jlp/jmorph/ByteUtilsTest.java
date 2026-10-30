// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ByteUtilsTest {

    @Test
    public void testChsize() {
        // setup
        byte b254 = -2;
        byte b191 = -65;
        int expected1 = 1;

        byte b192 = -64;
        byte b223 = -33;
        int expected2 = 2;

        byte b224 = -32;
        byte b239 = -17;
        int expected3 = 3;

        byte b240 = -16;
        byte b247 = -9;
        int expected4 = 4;

        byte b248 = -8;
        byte b251 = -5;
        int expected5 = 5;

        byte b252 = -4;
        byte b253 = -3;
        int expected6 = 6;

        // exec
        int actual254 = ByteUtils.chsize(b254);
        int actual191 = ByteUtils.chsize(b191);
        int actual192 = ByteUtils.chsize(b192);
        int actual223 = ByteUtils.chsize(b223);
        int actual224 = ByteUtils.chsize(b224);
        int actual239 = ByteUtils.chsize(b239);
        int actual240 = ByteUtils.chsize(b240);
        int actual247 = ByteUtils.chsize(b247);
        int actual248 = ByteUtils.chsize(b248);
        int actual251 = ByteUtils.chsize(b251);
        int actual252 = ByteUtils.chsize(b252);
        int actual253 = ByteUtils.chsize(b253);

        // verify
        assertEquals(expected1, actual254);
        assertEquals(expected1, actual191);
        assertEquals(expected2, actual192);
        assertEquals(expected2, actual223);
        assertEquals(expected3, actual224);
        assertEquals(expected3, actual239);
        assertEquals(expected4, actual240);
        assertEquals(expected4, actual247);
        assertEquals(expected5, actual248);
        assertEquals(expected5, actual251);
        assertEquals(expected6, actual252);
        assertEquals(expected6, actual253);
    }

    @Test
    public void testQuote() {
        // setup
        byte[] text1 = "a1あ".getBytes(StandardCharsets.UTF_8);
        byte[] expected1 = "a1あ".getBytes(StandardCharsets.UTF_8);

        byte[] text2 = "a1,あ".getBytes(StandardCharsets.UTF_8);
        byte[] expected2 = "\"a1,あ\"".getBytes(StandardCharsets.UTF_8);

        // exec
        byte[] actual1 = ByteUtils.quote(text1);
        byte[] actual2 = ByteUtils.quote(text2);

        // verify
        assertEquals(new String(expected1, StandardCharsets.UTF_8), new String(actual1, StandardCharsets.UTF_8));
        assertEquals(expected1.length, actual1.length);
        for (int i = 0; i < expected1.length; i++) {
            assertEquals(expected1[i], actual1[i]);
        }
        assertEquals(new String(expected2, StandardCharsets.UTF_8), new String(actual2, StandardCharsets.UTF_8));
        assertEquals(expected2.length, actual2.length);
        for (int i = 0; i < expected2.length; i++) {
            assertEquals(expected2[i], actual2[i]);
        }
    }

    @Test
    public void testJoin() {
        // setup
        byte[] delim = ",".getBytes(StandardCharsets.UTF_8);
        List<byte[]> elem = new ArrayList<byte[]>(Arrays.asList(
                "あいう".getBytes(StandardCharsets.UTF_8),
                "123".getBytes(StandardCharsets.UTF_8),
                "abc".getBytes(StandardCharsets.UTF_8)));
        byte[] expected = "あいう,123,abc".getBytes(StandardCharsets.UTF_8);

        // exec
        byte[] actual = ByteUtils.join(delim, elem);

        // verify
        assertEquals(new String(expected, StandardCharsets.UTF_8), new String(actual, StandardCharsets.UTF_8));
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    @Test
    public void testValueOf() {
        // setup
        int in = 2;
        byte[] expected = "2".getBytes(StandardCharsets.UTF_8);

        // exec
        byte[] actual = ByteUtils.valueOf(in);

        // verify
        assertEquals(new String(expected, StandardCharsets.UTF_8), new String(actual, StandardCharsets.UTF_8));
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    @Test
    public void testToBytes() {
        // setup
        String str = "あいう";
        byte[] expected = "あいう".getBytes(StandardCharsets.UTF_8);

        // exec
        byte[] actual = ByteUtils.toBytes(str);

        // verify
        assertEquals(new String(expected, StandardCharsets.UTF_8), new String(actual, StandardCharsets.UTF_8));
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    @Test
    public void testToStringB() {
        // setup
        byte[] b = "あいう".getBytes(StandardCharsets.UTF_8);
        String expected = "あいう";

        // exec
        String actual = ByteUtils.toString(b);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testToStringBol() {
        // setup
        byte[] b = "あいう".getBytes(StandardCharsets.UTF_8);
        int offset = "あ".getBytes(StandardCharsets.UTF_8).length;
        int length = "いう".getBytes(StandardCharsets.UTF_8).length;
        String expected = "いう";

        // exec
        String actual = ByteUtils.toString(b, offset, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testBytencmpByte() {
        // setup
        byte[] array = "aいuえお".getBytes(StandardCharsets.UTF_8);
        byte[] b1 = "いuえお".getBytes(StandardCharsets.UTF_8);
        int p1 = "い".getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer b2 = ByteBuffer.wrap(array);
        int p2 = "aい".getBytes(StandardCharsets.UTF_8).length;
        int n = 3;
        int expected = 0;

        // exec
        int actual = ByteUtils.bytencmp(b1, p1, b2, p2, n);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testBytencmpByteBuffer() {
        // setup
        byte[] array1 = "いuえお".getBytes(StandardCharsets.UTF_8);
        ByteBuffer b1 = ByteBuffer.wrap(array1);
        int p1 = "い".getBytes(StandardCharsets.UTF_8).length;
        byte[] array2 = "aいuえお".getBytes(StandardCharsets.UTF_8);
        ByteBuffer b2 = ByteBuffer.wrap(array2);
        int p2 = "aい".getBytes(StandardCharsets.UTF_8).length;
        int n = 3;
        int expected = 0;

        // exec
        int actual = ByteUtils.bytencmp(b1, p1, b2, p2, n);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testIntToBytesLE() {
        // exec
        byte[] actual = ByteUtils.intToBytesLE(0x12345678);

        // verify
        assertEquals((byte) 0x78, actual[0]);
        assertEquals((byte) 0x56, actual[1]);
        assertEquals((byte) 0x34, actual[2]);
        assertEquals((byte) 0x12, actual[3]);
    }

    @Test
    public void testIntToBytesLENegative() {
        // exec
        byte[] actual = ByteUtils.intToBytesLE(0x89ABCDEF);

        // verify
        assertEquals((byte) 0xEF, actual[0]);
        assertEquals((byte) 0xCD, actual[1]);
        assertEquals((byte) 0xAB, actual[2]);
        assertEquals((byte) 0x89, actual[3]);
    }
}
