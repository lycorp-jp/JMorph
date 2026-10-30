// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JMorphAnalyzerJsonrpcTest {

    JMorphAnalyzerJsonrpc target;

    @BeforeEach
    public void setUp() throws Exception {
        String modelPath = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        DictionaryConfig dictionaryConfig = DictionaryConfig.builder()
                .baseDic(new MorphDicMecabConfig(modelPath))
                .unknownDic(new UnknownDicBasicConfig(ConcatMode.ALL_EXCEPT_KATAKANA))
                .userDicInRequestProvider(new UserDicInRequestProviderBasicConfig())
                .build();
        AnalyzerConfig analyzerConfig = AnalyzerConfig.builder()
                .dictionary(dictionaryConfig)
                .nbestNum(1)
                .outputMode(OutputMode.ALL)
                .build();
        JsonrpcConfig jsonrpcConfig = JsonrpcConfig.builder()
                .method("jmorph.tokenize")
                .maxObject(16)
                .build();
        JMorphConfig config = JMorphConfig.builder()
                .analyzer(analyzerConfig)
                .jsonrpc(jsonrpcConfig)
                .build();
        target = new JMorphAnalyzerJsonrpc(config);
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testAnalyze() throws IOException {
        // setup
        String input = "{\"id\": \"2\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"東京特許許可局は実在しない\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        JsonNode tokens = root.get("result").get("tokens");
        assertEquals("2", root.get("id").asText());
        assertEquals("2.0", root.get("jsonrpc").asText());
        assertEquals(13, tokens.size());
        assertEquals("東", tokens.get(0).get(0).asText());
        assertEquals("品詞0", tokens.get(0).get(1).asText());
    }

    @Test
    public void testAnalyzeFile() throws IOException {
        // setup
        String inputPath = this.getClass().getClassLoader().getResource("input.json").getPath();

        // exec
        String actual = target.analyzeFile(inputPath);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        JsonNode tokens = root.get("result").get("tokens");
        assertEquals("2", root.get("id").asText());
        assertEquals("2.0", root.get("jsonrpc").asText());
        assertEquals(13, tokens.size());
        assertEquals("東", tokens.get(0).get(0).asText());
        assertEquals("品詞0", tokens.get(0).get(1).asText());
    }

    @Test
    public void testAnalyzeDoesNotReadFile() throws IOException {
        // setup
        String inputPath = this.getClass().getClassLoader().getResource("input.json").getPath();

        // exec
        String actual = target.analyze(inputPath);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertTrue(root.get("id").isNull());
        assertEquals(-32700, root.get("error").get("code").asInt());
        assertEquals("Parse error", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeEmptyString() {
        // setup
        String input =
                "{\"id\": \"1\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"\" }}";
        String expected = "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"result\":{\"tokens\":[]}}";

        // exec
        String actual = target.analyze(input);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testAnalyzeNullQuery() throws IOException {
        // setup
        String input =
                "{\"id\": \"7\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": null }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("7", root.get("id").asText());
        assertEquals(-32602, root.get("error").get("code").asInt());
        assertEquals("Invalid params", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeNumericIdKeepsNumberType() throws IOException {
        // setup
        String input =
                "{\"id\": 1000, \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"花子\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertTrue(root.get("id").isNumber());
        assertEquals(1000, root.get("id").asInt());
    }

    @Test
    public void testAnalyzeMissingParams() throws IOException {
        // setup
        String input = "{\"id\": \"8\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\"}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("8", root.get("id").asText());
        assertEquals(-32602, root.get("error").get("code").asInt());
        assertEquals("Invalid params", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeInvalidContextEntries() throws IOException {
        // setup
        String input =
                "{\"id\": \"9\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"花子\", \"context\": { \"entries\": \"花子\" } }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("9", root.get("id").asText());
        assertEquals(-32602, root.get("error").get("code").asInt());
        assertEquals("Invalid params", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeBatchRequestIsNotSupported() throws IOException {
        // setup
        String input =
                "[{\"id\": \"10\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"花子\" }}]";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertTrue(root.get("id").isNull());
        assertEquals(-32600, root.get("error").get("code").asInt());
        assertEquals("Invalid request", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeNotificationIsNotSupported() throws IOException {
        // setup
        String input = "{\"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"花子\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertTrue(root.get("id").isNull());
        assertEquals(-32600, root.get("error").get("code").asInt());
        assertEquals("Invalid request", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeInvalidMethod() throws IOException {
        // setup
        String input =
                "{\"id\": \"3\", \"jsonrpc\": \"2.0\", \"method\": \"unknown\", \"params\": { \"q\": \"花子\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("3", root.get("id").asText());
        assertEquals(-32601, root.get("error").get("code").asInt());
        assertEquals("Method not found", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeInvalidJsonrpcVersion() throws IOException {
        // setup
        String input =
                "{\"id\": \"4\", \"jsonrpc\": \"1.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"花子\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("4", root.get("id").asText());
        assertEquals(-32600, root.get("error").get("code").asInt());
        assertEquals("Invalid request", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeMissingMethod() throws IOException {
        // setup
        String input =
                "{\"id\": \"5\", \"jsonrpc\": \"2.0\", \"params\": { \"q\": \"花子\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("5", root.get("id").asText());
        assertEquals(-32600, root.get("error").get("code").asInt());
        assertEquals("Invalid request", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeInvalidIdType() throws IOException {
        // setup
        String input =
                "{\"id\": {\"value\": \"11\"}, \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"花子\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertTrue(root.get("id").isNull());
        assertEquals(-32600, root.get("error").get("code").asInt());
        assertEquals("Invalid request", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeInvalidJsonrpcType() throws IOException {
        // setup
        String input =
                "{\"id\": \"12\", \"jsonrpc\": 2.0, \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"花子\" }}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("12", root.get("id").asText());
        assertEquals(-32600, root.get("error").get("code").asInt());
        assertEquals("Invalid request", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeInvalidParamsType() throws IOException {
        // setup
        String input = "{\"id\": \"13\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": []}";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertEquals("13", root.get("id").asText());
        assertEquals(-32602, root.get("error").get("code").asInt());
        assertEquals("Invalid params", root.get("error").get("message").asText());
    }

    @Test
    public void testAnalyzeMalformedJson() throws IOException {
        // setup
        String input = "{\"id\": \"6\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\"";

        // exec
        String actual = target.analyze(input);

        // verify
        JsonNode root = new ObjectMapper().readTree(actual);
        assertTrue(root.get("id").isNull());
        assertEquals(-32700, root.get("error").get("code").asInt());
        assertEquals("Parse error", root.get("error").get("message").asText());
    }
}
