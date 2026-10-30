// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JMorphAnalyzerTest {

    private JMorphAnalyzer target;

    @BeforeEach
    public void setUp() throws IOException {
        String modelPath = this.getClass().getClassLoader().getResource(
                Paths.get("mecab-small-dic", "final").toString()).getPath();
        DictionaryConfig dictionaryConfig = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig(modelPath))
                .unknownDic(new UnknownDicBasicConfig(ConcatMode.ALL_EXCEPT_KATAKANA))
                .userDicInRequestProvider(new UserDicInRequestProviderBasicConfig())
                .build();
        AnalyzerConfig analyzerConfig = AnalyzerConfig.builder()
                .dictionary(dictionaryConfig)
                .nbestNum(2)
                .outputMode(OutputMode.ALL)
                .build();
        target = new JMorphAnalyzer(JMorphConfig.builder().analyzer(analyzerConfig).build());
    }

    @Test
    public void testAnalyzeEmpty() {
        // exec
        List<TokenInfo> actual = target.analyze(new byte[0]);

        // verify
        assertEquals(Collections.emptyList(), actual);
    }

    @Test
    public void testAnalyzeEmptyClearsPreviousLattice() {
        // 注意: テスト用辞書に「太郎」と「太」+「郎」の2経路があることが前提
        target.analyze("太郎");
        assertFalse(target.next().isEmpty());

        target.analyze("太郎");
        target.analyze("");
        List<TokenInfo> actual = target.next();

        assertEquals(Collections.emptyList(), actual);
    }

    @Test
    public void testAnalyzeNull() {
        // exec & verify
        assertThrows(NullPointerException.class, () -> target.analyze((String) null));
    }

    @Test
    public void testAnalyze() {
        // exec
        List<TokenInfo> actual = target.analyze("花子");

        // verify
        assertEquals(2, actual.size());
        assertEquals("花子", actual.get(0).get()[0]);
        assertEquals("品詞1", actual.get(0).get()[1]);
        assertEquals("EOS", actual.get(1).get()[0]);
    }

    @Test
    public void testNextBeforeAnalyze() {
        // exec
        List<TokenInfo> actual = target.next();

        // verify
        assertEquals(Collections.emptyList(), actual);
    }

    @Test
    public void testAnalyzeStopsWhenTimeoutIsExceeded() throws IOException {
        String modelPath = this.getClass().getClassLoader().getResource(
                Paths.get("mecab-small-dic", "final").toString()).getPath();
        DictionaryConfig dictionaryConfig = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig(modelPath))
                .unknownDic(new UnknownDicBasicConfig(ConcatMode.ALL_EXCEPT_KATAKANA))
                .userDicInRequestProvider(new UserDicInRequestProviderBasicConfig())
                .build();
        AnalyzerConfig analyzerConfig = AnalyzerConfig.builder()
                .dictionary(dictionaryConfig)
                .timeoutMillis(1)
                .build();
        JMorphConfig config = JMorphConfig.builder().analyzer(analyzerConfig).build();
        JMorphResource resource = new JMorphResourceLoaderImpl().load(config);
        List<UserDicEntry> entries = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entries.add(new UserDicEntry("a"));
        }
        List<MorphDic> morphDicList = new ArrayList<>(resource.getMorphDicList());
        morphDicList.add(resource.getUserDicInRequestProvider().create(entries));
        JMorphResource resourceWithUserDic = new JMorphResource(
                morphDicList, resource.getCostMatrix(), resource.getUnknownDic(),
                resource.getUserDicInRequestProvider());
        JMorphAnalyzer analyzer = new JMorphAnalyzer(config, resourceWithUserDic);

        assertThrows(TimeoutExceededException.class, () -> analyzer.analyze("a".repeat(1000)));
        assertEquals(Collections.emptyList(), analyzer.next());
    }

}
