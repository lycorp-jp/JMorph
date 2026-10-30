// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DictionaryConfigTest {

    @Test
    void testMissingAndNullUnknownDicUseBasicDefaults() {
        String prefix = "{\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"}";

        assertBasicDefaults(prefix + "}}}");
        assertBasicDefaults(prefix + ",\"unknownDic\":null}}}");
    }

    @Test
    void testEmptyUnknownDicIsRejected() {
        String json = "{\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"},\"unknownDic\":{}}}}";

        assertThrows(RuntimeException.class, () -> JMorphConfig.fromJson(json));
    }

    private void assertBasicDefaults(String json) {
        UnknownDicConfig unknownDic = JMorphConfig.fromJson(json).getAnalyzerConfig().getDictionary().unknownDic();

        UnknownDicBasicConfig basic = assertInstanceOf(UnknownDicBasicConfig.class, unknownDic);
        assertEquals(ConcatMode.ALL, basic.getConcatMode());
        assertEquals(" ", basic.getDelim());
    }

    @Test
    void testUnknownDicRejectsUnknownType() {
        String json = "{\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"},"
                + "\"unknownDic\":{\"type\":\"unknown\"}}}}";

        assertThrows(RuntimeException.class, () -> JMorphConfig.fromJson(json));
    }
}
