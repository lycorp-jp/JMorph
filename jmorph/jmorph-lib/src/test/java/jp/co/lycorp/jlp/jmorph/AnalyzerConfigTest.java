// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AnalyzerConfigTest {

    @Test
    void testNbestNumMustBePositive() {
        DictionaryConfig dictionary = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig("dummy"))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> AnalyzerConfig.builder().dictionary(dictionary).nbestNum(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> AnalyzerConfig.builder().dictionary(dictionary).nbestNum(-1).build());
    }

    @Test
    void testDictionaryIsRequiredForAllCreationPaths() {
        assertThrows(IllegalStateException.class,
                () -> AnalyzerConfig.builder().build());
        assertThrows(RuntimeException.class,
                () -> JMorphConfig.fromJson("{\"analyzer\":{}}"));
    }

    @Test
    void testNbestNumDefaultsWhenMissing() {
        String json = "{\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"}}}}";

        JMorphConfig config = JMorphConfig.fromJson(json);

        assertEquals(1, config.getAnalyzerConfig().getNbestNum());
    }

    @Test
    void testJsonNbestNumMustBePositive() {
        String json = "{\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"}},\"nbestNum\":0}}";

        assertThrows(RuntimeException.class, () -> JMorphConfig.fromJson(json));
    }

    @Test
    void testTimeoutMillisDefaultsWhenMissing() {
        String json = "{\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"}}}}";

        JMorphConfig config = JMorphConfig.fromJson(json);

        assertEquals(10_000L, config.getAnalyzerConfig().getTimeoutMillis());
    }

    @Test
    void testTimeoutMillisCanBeConfiguredAndDisabled() {
        DictionaryConfig dictionary = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig("dummy"))
                .build();

        AnalyzerConfig enabled = AnalyzerConfig.builder()
                .dictionary(dictionary)
                .timeoutMillis(30_000L)
                .build();
        AnalyzerConfig disabled = AnalyzerConfig.builder()
                .dictionary(dictionary)
                .timeoutMillis(0)
                .build();

        assertEquals(30_000L, enabled.getTimeoutMillis());
        assertEquals(0, disabled.getTimeoutMillis());
    }

    @Test
    void testJsonTimeoutMillisCanBeConfigured() {
        String json = "{\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"}},\"timeoutMillis\":30000}}";

        JMorphConfig config = JMorphConfig.fromJson(json);

        assertEquals(30_000L, config.getAnalyzerConfig().getTimeoutMillis());
    }

    @Test
    void testTimeoutMillisMustNotBeNegative() {
        DictionaryConfig dictionary = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig("dummy"))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> AnalyzerConfig.builder().dictionary(dictionary).timeoutMillis(-1).build());
    }
}
