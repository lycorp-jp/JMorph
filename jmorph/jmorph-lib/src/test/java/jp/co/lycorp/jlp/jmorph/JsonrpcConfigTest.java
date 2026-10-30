// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonrpcConfigTest {

    @Test
    void testMaxObjectMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> JsonrpcConfig.builder().maxObject(0).build());
        assertThrows(IllegalArgumentException.class, () -> JsonrpcConfig.builder().maxObject(-1).build());
    }

    @Test
    void testMaxObjectDefaultsWhenMissing() throws Exception {
        JsonrpcConfig config = new ObjectMapper().readValue("{\"method\":\"jmorph.tokenize\"}",
                JsonrpcConfig.class);

        assertEquals(16, config.getMaxObject());
    }

    @Test
    void testDefaultsAreSameWithAndWithoutJsonrpc() {
        String analyzer = "\"analyzer\":{\"dictionary\":{\"baseDic\":"
                + "{\"type\":\"mecab\",\"location\":\"dummy\"}}}";

        JMorphConfig withoutJsonrpc = JMorphConfig.fromJson("{" + analyzer + "}");
        JMorphConfig withEmptyJsonrpc = JMorphConfig.fromJson("{" + analyzer + ",\"jsonrpc\":{}}");

        assertEquals("jmorph.tokenize", withoutJsonrpc.getJsonrpcConfig().getMethod());
        assertEquals("jmorph.tokenize", withEmptyJsonrpc.getJsonrpcConfig().getMethod());
        assertEquals(16, withoutJsonrpc.getJsonrpcConfig().getMaxObject());
        assertEquals(16, withEmptyJsonrpc.getJsonrpcConfig().getMaxObject());
    }

    @Test
    void testJsonMaxObjectMustBePositive() {
        ObjectMapper mapper = new ObjectMapper();

        assertThrows(JsonMappingException.class,
                () -> mapper.readValue("{\"method\":\"jmorph.tokenize\",\"maxObject\":0}", JsonrpcConfig.class));
    }
}
