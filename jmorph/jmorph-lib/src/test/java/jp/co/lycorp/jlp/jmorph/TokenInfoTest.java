// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class TokenInfoTest {

    @Test
    public void testGet() {
        // setup
        byte[][] result = {
                "美しい".getBytes(StandardCharsets.UTF_8),
                "1234".getBytes(StandardCharsets.UTF_8),
                "5678".getBytes(StandardCharsets.UTF_8),
                "うつくしい".getBytes(StandardCharsets.UTF_8),
                "美しい".getBytes(StandardCharsets.UTF_8),
                "品詞0".getBytes(StandardCharsets.UTF_8),
                "詳細品詞1".getBytes(StandardCharsets.UTF_8),
                "活用型1".getBytes(StandardCharsets.UTF_8),
                "活用形1".getBytes(StandardCharsets.UTF_8)
        };
        TokenInfoImpl target = new TokenInfoImpl(result);
        String[] expected = {
                "美しい",
                "1234",
                "5678",
                "うつくしい",
                "美しい",
                "品詞0",
                "詳細品詞1",
                "活用型1",
                "活用形1"
        };

        // exec
        String[] actual = target.get();

        // verify
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testGetBytes() {
        // setup
        byte[][] result = {
                "美しい".getBytes(StandardCharsets.UTF_8),
                "1234".getBytes(StandardCharsets.UTF_8),
                "5678".getBytes(StandardCharsets.UTF_8),
                "うつくしい".getBytes(StandardCharsets.UTF_8),
                "美しい".getBytes(StandardCharsets.UTF_8),
                "品詞0".getBytes(StandardCharsets.UTF_8),
                "詳細品詞1".getBytes(StandardCharsets.UTF_8),
                "活用型1".getBytes(StandardCharsets.UTF_8),
                "活用形1".getBytes(StandardCharsets.UTF_8)
        };
        TokenInfoImpl target = new TokenInfoImpl(result);
        byte[][] expected = {
                "美しい".getBytes(StandardCharsets.UTF_8),
                "1234".getBytes(StandardCharsets.UTF_8),
                "5678".getBytes(StandardCharsets.UTF_8),
                "うつくしい".getBytes(StandardCharsets.UTF_8),
                "美しい".getBytes(StandardCharsets.UTF_8),
                "品詞0".getBytes(StandardCharsets.UTF_8),
                "詳細品詞1".getBytes(StandardCharsets.UTF_8),
                "活用型1".getBytes(StandardCharsets.UTF_8),
                "活用形1".getBytes(StandardCharsets.UTF_8)
        };

        // exec
        byte[][] actual = target.getBytes();

        // verify
        assertArrayEquals(expected, actual);
    }
}
