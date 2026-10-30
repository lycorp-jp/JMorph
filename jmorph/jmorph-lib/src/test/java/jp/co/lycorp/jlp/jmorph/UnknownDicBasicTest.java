// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class UnknownDicBasicTest {

    UnknownDicBasic unknownDic;

    private UnknownDicBasic createUnknownDic(String modelName, ConcatMode concatMode) throws IOException {
        UnknownDicBasicConfig config = new UnknownDicBasicConfig(null, concatMode, " ");
        return new UnknownDicBasic(
                FileUtils.resolve(modelName, "unk.csv"),
                FileUtils.resolve(modelName, "dump.unknown"),
                false,
                config);
    }

    @Test
    public void testUnkNode() throws IOException {
        // setup
        int pos = 0;
        byte[] sentence = "34567".getBytes(StandardCharsets.UTF_8);

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        // exec
        NodeImpl actual = unknownDic.unkNode(sentence, pos);

        // verify
        assertEquals(1, actual.token.getLc());
        assertEquals(1, actual.token.getRc());
        assertEquals(255, actual.token.getCost());
        assertEquals(1, actual.token.getFeatureSetId());
        assertEquals(-1, actual.dictId);
        assertEquals(pos, actual.loc);
        assertEquals(Math.min(ByteUtils.chsize(sentence[pos]),
                "34567".getBytes(StandardCharsets.UTF_8).length - pos), actual.length);
    }

    @Test
    public void testGetConcatTypeConcatFalse() throws IOException {
        // setup
        byte[] sentence = "34567".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.NONE);

        String expected = "o";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueNumerals() throws IOException {
        // setup
        byte[] sentence = "34567".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        String expected = "n";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueNumeralsFullWidth() throws IOException {
        // setup
        byte[] sentence = "３４５６７".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        String expected = "n";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueNumeralsKanji() throws IOException {
        // setup
        byte[] sentence = "三兆四億五千六百七十八".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        String expected = "N";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueAlphabet() throws IOException {
        // setup
        byte[] sentence = "vnsofn".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        String expected = "a";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueAlphabetFullWidth() throws IOException {
        // setup
        byte[] sentence = "ｃｑｂｏｅｕｖｂｏ".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        String expected = "a";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueHiragana() throws IOException {
        // setup
        byte[] sentence = "んがあ".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        String expected = "o"; // h（ひらがな）はconcatSetに含まれないため

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueNcckFalseKatakana() throws IOException {
        // setup
        byte[] sentence = "ンガア".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        String expected = "k";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testGetConcatTypeConcatTrueNcckTrueKatakana() throws IOException {
        // setup
        byte[] sentence = "ンガア".getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        int length = sentence.length;

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL_EXCEPT_KATAKANA);

        String expected = "o";

        // exec
        String actual = unknownDic.getConcatType(sentence, pos, length);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testShouldConcatConcatFalseNcckFalse() throws IOException {
        // setup
        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.NONE);

        // exec
        boolean expected_a = unknownDic.shouldConcat("a");
        boolean expected_n = unknownDic.shouldConcat("n");
        boolean expected_N = unknownDic.shouldConcat("N");
        boolean expected_k = unknownDic.shouldConcat("k");
        boolean expected_h = unknownDic.shouldConcat("h");
        boolean expected_c = unknownDic.shouldConcat("c");

        // verify
        assertFalse(expected_a);
        assertFalse(expected_n);
        assertFalse(expected_N);
        assertFalse(expected_k);
        assertFalse(expected_h);
        assertFalse(expected_c);
    }

    @Test
    public void testShouldConcatConcatTrueNcckFalse() throws IOException {
        // setup
        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL);

        // exec
        boolean expected_a = unknownDic.shouldConcat("a");
        boolean expected_n = unknownDic.shouldConcat("n");
        boolean expected_N = unknownDic.shouldConcat("N");
        boolean expected_k = unknownDic.shouldConcat("k");
        boolean expected_h = unknownDic.shouldConcat("h");
        boolean expected_c = unknownDic.shouldConcat("c");

        // verify
        assertTrue(expected_a);
        assertTrue(expected_n);
        assertTrue(expected_N);
        assertTrue(expected_k);
        assertFalse(expected_h);
        assertFalse(expected_c);
    }

    @Test
    public void testShouldConcatConcatTrueNcckTrue() throws IOException {
        // setup
        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL_EXCEPT_KATAKANA);

        // exec
        boolean expected_a = unknownDic.shouldConcat("a");
        boolean expected_n = unknownDic.shouldConcat("n");
        boolean expected_N = unknownDic.shouldConcat("N");
        boolean expected_k = unknownDic.shouldConcat("k");
        boolean expected_h = unknownDic.shouldConcat("h");
        boolean expected_c = unknownDic.shouldConcat("c");

        // verify
        assertTrue(expected_a);
        assertTrue(expected_n);
        assertTrue(expected_N);
        assertFalse(expected_k);
        assertFalse(expected_h);
        assertFalse(expected_c);
    }

    @Test
    public void testConcat() throws IOException {
        // setup
        NodeImpl node1 = new NodeImpl(0, 1);
        NodeImpl node2 = new NodeImpl(1, 1);
        NodeImpl node3 = new NodeImpl(2, 1);
        List<NodeImpl> nodeList = Arrays.asList(node1, node2, node3);
        NodeImpl expected = new NodeImpl(0, 3);

        String modelName = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.ALL_EXCEPT_KATAKANA);

        // exec
        NodeImpl actual = unknownDic.concat(nodeList);

        // verify
        assertEquals(expected.loc, actual.loc);
        assertEquals(expected.length, actual.length);
    }

    @Test
    public void testGetFeaturesByConfigOutputFeaturesFalse() throws IOException {
        // setup
        String modelName = this.getClass().getClassLoader()
                .getResource("mecab-small-dic/final").getPath();
        unknownDic = createUnknownDic(modelName, ConcatMode.NONE);

        // verify
        assertThrows(
                IllegalStateException.class,
                () -> unknownDic.getFeaturesByConfig("abc", null));
    }
}
