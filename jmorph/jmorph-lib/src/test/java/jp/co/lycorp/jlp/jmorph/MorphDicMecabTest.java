// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MorphDicMecabTest {

    private MorphDicMecab target;

    @BeforeEach
    public void setUp() throws Exception {
        String modelPath = this.getClass().getClassLoader()
                .getResource(Paths.get("mecab-small-dic", "final").toString()).getPath();
        target = new MorphDicMecab(modelPath, true);
    }

    @Test
    public void testLookup() {
        byte[] sentence = "花子".getBytes(StandardCharsets.UTF_8);

        List<NodeImpl> actual = target.lookup(sentence, 0, 0);

        assertEquals(1, actual.size());
        assertEquals(0, actual.get(0).loc);
        assertEquals(6, actual.get(0).length);
    }

    @Test
    public void testLookupWithOffset() {
        byte[] prefix = "彼は".getBytes(StandardCharsets.UTF_8);
        byte[] sentence = "彼は花子".getBytes(StandardCharsets.UTF_8);

        List<NodeImpl> actual = target.lookup(sentence, prefix.length, 0);

        assertEquals(1, actual.size());
        assertEquals(prefix.length, actual.get(0).loc);
        assertEquals(6, actual.get(0).length);
    }

    @Test
    public void testGetFeatures() {
        TokenMecab token = new TokenMecab((short) 4, (short) 4, (short) 417, 113);
        NodeImpl node = new NodeImpl(token, 0, 0, 6);
        String[] expected = {"品詞1", "詳細品詞1", "はなこ"};

        byte[][] actual = target.getFeatures(node);

        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            assertEquals(expected[i], ByteUtils.toString(actual[i]));
        }
    }

    @Test
    public void testGetFeaturesWithNullBuffer() throws IOException {
        String modelPath = this.getClass().getClassLoader()
                .getResource(Paths.get("mecab-small-dic", "final").toString()).getPath();
        MorphDicMecab dic = new MorphDicMecab(modelPath, false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dic.getFeatures(new NodeImpl(1, 1)));

        assertTrue(exception.getMessage().contains("Feature Buffer has not been initialized"));
    }

    @Test
    public void testDump() throws Exception {
        assertDictionaryBuffers(target, true);
    }

    @Test
    public void testNoDump() throws Exception {
        String modelPath = this.getClass().getClassLoader()
                .getResource(Paths.get("mecab-small-dic", "final").toString()).getPath();
        MorphDicMecab dic = new MorphDicMecab(modelPath, false);

        assertDictionaryBuffers(dic, false);
    }

    @Test
    public void testRejectsSectionSizesLargerThanFileBeforeAllocation(@TempDir Path tempDir) throws Exception {
        int[] sizes = readDictionarySectionSizes();
        Path dictionaryPath = copyDictionaryWithSectionSizes(
                tempDir, Integer.MAX_VALUE - 7, sizes[1], sizes[2]);

        IOException exception = assertThrows(
                IOException.class,
                () -> new MorphDicMecab(dictionaryPath.toString(), true));

        assertTrue(exception.getCause().getMessage().contains("section sizes do not match file size"));
    }

    @Test
    public void testRejectsNegativeSectionSize(@TempDir Path tempDir) throws Exception {
        int[] sizes = readDictionarySectionSizes();
        Path dictionaryPath = copyDictionaryWithSectionSizes(tempDir, sizes[0], -1, sizes[2]);

        IOException exception = assertThrows(
                IOException.class,
                () -> new MorphDicMecab(dictionaryPath.toString(), true));

        assertTrue(exception.getCause().getMessage().contains("negative section size"));
    }

    @Test
    public void testRejectsMisalignedDartsSectionSize(@TempDir Path tempDir) throws Exception {
        int[] sizes = readDictionarySectionSizes();
        Path dictionaryPath = copyDictionaryWithSectionSizes(
                tempDir, sizes[0] + 1, sizes[1], sizes[2] - 1);

        IOException exception = assertThrows(
                IOException.class,
                () -> new MorphDicMecab(dictionaryPath.toString(), true));

        assertTrue(exception.getCause().getMessage().contains("invalid Darts section size"));
    }

    @Test
    public void testRejectsMisalignedTokenSectionSize(@TempDir Path tempDir) throws Exception {
        int[] sizes = readDictionarySectionSizes();
        Path dictionaryPath = copyDictionaryWithSectionSizes(
                tempDir, sizes[0], sizes[1] + 1, sizes[2] - 1);

        IOException exception = assertThrows(
                IOException.class,
                () -> new MorphDicMecab(dictionaryPath.toString(), true));

        assertTrue(exception.getCause().getMessage().contains("invalid token section size"));
    }

    private int[] readDictionarySectionSizes() throws Exception {
        byte[] dictionary = Files.readAllBytes(getDictionaryFilePath());
        ByteBuffer buffer = ByteBuffer.wrap(dictionary).order(ByteOrder.LITTLE_ENDIAN);
        return new int[] {buffer.getInt(24), buffer.getInt(28), buffer.getInt(32)};
    }

    private Path copyDictionaryWithSectionSizes(
            Path tempDir, int dSize, int tSize, int fSize) throws Exception {
        byte[] dictionary = Files.readAllBytes(getDictionaryFilePath());
        ByteBuffer buffer = ByteBuffer.wrap(dictionary).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(24, dSize);
        buffer.putInt(28, tSize);
        buffer.putInt(32, fSize);
        Files.write(tempDir.resolve("sys.dic"), dictionary);
        return tempDir;
    }

    private Path getDictionaryFilePath() {
        String modelPath = this.getClass().getClassLoader()
                .getResource(Paths.get("mecab-small-dic", "final").toString()).getPath();
        return Paths.get(modelPath, "sys.dic");
    }

    private void assertDictionaryBuffers(MorphDicMecab dic, boolean featuresEnabled) throws Exception {
        Field dartsField = MorphDicMecab.class.getDeclaredField("darts");
        Field tokenBufferField = MorphDicMecab.class.getDeclaredField("tokenBuffer");
        Field featureBufferField = MorphDicMecab.class.getDeclaredField("featureBuffer");
        dartsField.setAccessible(true);
        tokenBufferField.setAccessible(true);
        featureBufferField.setAccessible(true);

        assertNotNull(dartsField.get(dic));
        assertNotNull(tokenBufferField.get(dic));
        if (featuresEnabled) {
            assertNotNull(featureBufferField.get(dic));
        } else {
            assertNull(featureBufferField.get(dic));
        }
    }
}
