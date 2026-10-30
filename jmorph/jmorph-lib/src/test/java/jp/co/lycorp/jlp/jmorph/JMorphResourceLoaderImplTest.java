// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JMorphResourceLoaderImplTest {

    @Test
    void testLoadAdditionalDic() throws Exception {
        String modelPath = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        DictionaryConfig dictionaryConfig = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig(modelPath))
                .additionalDic(new MorphDicMecabConfig(modelPath))
                .build();
        AnalyzerConfig analyzerConfig = AnalyzerConfig.builder()
                .dictionary(dictionaryConfig)
                .outputMode(OutputMode.ALL)
                .build();
        JMorphConfig config = JMorphConfig.builder()
                .analyzer(analyzerConfig)
                .build();

        JMorphResource resource = new JMorphResourceLoaderImpl().load(config);

        assertEquals(2, resource.getMorphDicList().size());
        Dictionary dictionary = new DictionaryImpl(resource.getMorphDicList(), resource.getCostMatrix(),
                resource.getUnknownDic(), resource.getUserDicInRequestProvider(), true);
        byte[] sentence = "花子".getBytes(StandardCharsets.UTF_8);
        List<NodeImpl> nodes = dictionary.lookup(sentence, 0);
        assertEquals(2, nodes.size());
        assertEquals(0, nodes.get(0).getDictId());
        assertEquals(1, nodes.get(1).getDictId());
        assertEquals("品詞1", ByteUtils.toString(dictionary.getAllFeatures(sentence, nodes.get(1)).get(0)[1]));
    }

    @Test
    void testRejectsAdditionalDicWithOutOfRangeLc(@TempDir Path additionalDicPath) throws Exception {
        Path modelPath = getModelPath();
        writeAdditionalDic(modelPath, additionalDicPath, 0, (short) 11);

        IOException exception = assertThrows(IOException.class,
                () -> loadWithAdditionalDic(modelPath, additionalDicPath));

        assertTrue(exception.getCause().getMessage().contains("Dictionary connection ID is out of CostMatrix range"));
    }

    @Test
    void testRejectsAdditionalDicWithOutOfRangeRc(@TempDir Path additionalDicPath) throws Exception {
        Path modelPath = getModelPath();
        writeAdditionalDic(modelPath, additionalDicPath, Short.BYTES, (short) 11);

        IOException exception = assertThrows(IOException.class,
                () -> loadWithAdditionalDic(modelPath, additionalDicPath));

        assertTrue(exception.getCause().getMessage().contains("Dictionary connection ID is out of CostMatrix range"));
    }

    @Test
    void testLoadsAdditionalDicWithUserDicConnectionId(@TempDir Path additionalDicPath) throws Exception {
        Path modelPath = getModelPath();
        writeAdditionalDic(modelPath, additionalDicPath, 0, CostMatrixMecab.userDicEntriesIndex);
        writeAdditionalDic(additionalDicPath, additionalDicPath, Short.BYTES,
                CostMatrixMecab.userDicEntriesIndex);

        JMorphResource resource = loadWithAdditionalDic(modelPath, additionalDicPath);

        assertEquals(2, resource.getMorphDicList().size());
    }

    private Path getModelPath() throws Exception {
        return Paths.get(this.getClass().getClassLoader().getResource("mecab-small-dic/final").toURI());
    }

    private void writeAdditionalDic(Path sourcePath, Path additionalDicPath, int fieldOffset, short value)
            throws IOException {
        byte[] dictionary = Files.readAllBytes(sourcePath.resolve("sys.dic"));
        ByteBuffer buffer = ByteBuffer.wrap(dictionary).order(ByteOrder.LITTLE_ENDIAN);
        int dartsSize = buffer.getInt(Integer.BYTES * 6);
        int tokenOffset = Integer.BYTES * 10 + 32 + dartsSize;
        buffer.putShort(tokenOffset + fieldOffset, value);
        Files.write(additionalDicPath.resolve("sys.dic"), dictionary);
    }

    private JMorphResource loadWithAdditionalDic(Path modelPath, Path additionalDicPath) throws IOException {
        DictionaryConfig dictionaryConfig = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig(modelPath.toString()))
                .additionalDic(new MorphDicMecabConfig(additionalDicPath.toString()))
                .build();
        AnalyzerConfig analyzerConfig = AnalyzerConfig.builder()
                .dictionary(dictionaryConfig)
                .outputMode(OutputMode.ALL)
                .build();
        JMorphConfig config = JMorphConfig.builder().analyzer(analyzerConfig).build();
        return new JMorphResourceLoaderImpl().load(config);
    }
}
