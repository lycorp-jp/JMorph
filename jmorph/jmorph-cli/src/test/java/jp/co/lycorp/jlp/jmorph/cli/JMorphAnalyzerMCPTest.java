// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.co.lycorp.jlp.jmorph.TokenizerForJsonrpc;

public class JMorphAnalyzerMCPTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private String captureStdout(ThrowingRunnable runnable) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8.name()));
        try {
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
    }

    private TokenizerForJsonrpc createThrowingAnalyzer() {
        return new TokenizerForJsonrpc() {
            public String analyze(String jsonStr) {
                throw new RuntimeException("boom");
            }
        };
    }

    @Test
    public void testConvertCallJsonInput() throws Exception {
        String input = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":10,"
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "\"name\":\"jmorph.tokenize\","
                + "\"arguments\":{"
                + "\"q\":\"水車小屋で昔話を読んだ\","
                + "\"context\":{\"entries\":[\"水車小屋\",\"昔話,むかしばなし\"]}"
                + "}"
                + "}"
                + "}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonInput(MAPPER, MAPPER.readTree(input)));

        assertEquals("2.0", actual.path("jsonrpc").asText());
        assertEquals(10, actual.path("id").asInt());
        assertTrue(actual.path("id").isInt());
        assertEquals("jmorph.tokenize", actual.path("method").asText());
        assertEquals("水車小屋で昔話を読んだ", actual.path("params").path("q").asText());
        assertEquals("水車小屋", actual.path("params").path("context").path("entries").get(0).asText());
        assertEquals("昔話,むかしばなし", actual.path("params").path("context").path("entries").get(1).asText());
    }

    @Test
    public void testConvertCallJsonInputPreservesStringIdWithoutContext() throws Exception {
        String input = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"request-1\","
                + "\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{\"q\":\"水車\"}}"
                + "}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonInput(MAPPER, MAPPER.readTree(input)));

        assertEquals("2.0", actual.path("jsonrpc").asText());
        assertEquals("request-1", actual.path("id").asText());
        assertTrue(actual.path("id").isTextual());
        assertEquals("jmorph.tokenize", actual.path("method").asText());
        assertEquals("水車", actual.path("params").path("q").asText());
        assertFalse(actual.path("params").has("context"));
    }

    @Test
    public void testConvertCallJsonInputPreservesWhitespaceInValues() throws Exception {
        String input = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"request-1\","
                + "\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{"
                + "\"q\":\"水車 小屋\\n川辺\","
                + "\"context\":{\"entries\":[\"水車 小屋\",\"川辺\\n広場\"]}"
                + "}}"
                + "}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonInput(MAPPER, MAPPER.readTree(input)));

        assertEquals("水車 小屋\n川辺", actual.path("params").path("q").asText());
        assertEquals("水車 小屋", actual.path("params").path("context").path("entries").get(0).asText());
        assertEquals("川辺\n広場", actual.path("params").path("context").path("entries").get(1).asText());
    }

    @Test
    public void testValidateToolCallRequestAcceptsValidRequest() throws Exception {
        JsonNode request = MAPPER.readTree("{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"request-1\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "\"name\":\"jmorph.tokenize\","
                + "\"arguments\":{"
                + "\"q\":\"水車\","
                + "\"context\":{\"entries\":[\"水車\",\"昔話,むかしばなし\"]}"
                + "}"
                + "}"
                + "}");

        assertNull(JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsMissingParams() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\"}");

        assertEquals("Invalid params: params must be an object", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsMissingName() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"arguments\":{\"q\":\"水車\"}}}");

        assertEquals("Invalid params: params.name must be a string", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsUnsupportedToolName() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"unknown_tool\",\"arguments\":{\"q\":\"水車\"}}}");

        assertEquals("Invalid params: unsupported tool name: unknown_tool", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsMissingArguments() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\"}}");

        assertEquals("Invalid params: params.arguments must be an object", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsMissingQuery() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{}}}");

        assertEquals("Invalid params: params.arguments.q must be a string", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsNonStringQuery() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{\"q\":123}}}");

        assertEquals("Invalid params: params.arguments.q must be a string", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsNonObjectContext() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{\"q\":\"水車\",\"context\":[]}}}");

        assertEquals("Invalid params: params.arguments.context must be an object", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsNonArrayEntries() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{\"q\":\"水車\",\"context\":{\"entries\":\"水車\"}}}}");

        assertEquals("Invalid params: params.arguments.context.entries must be an array", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testValidateToolCallRequestRejectsNonStringEntry() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{\"q\":\"水車\",\"context\":{\"entries\":[\"水車\",123]}}}}");

        assertEquals("Invalid params: params.arguments.context.entries[1] must be a string", JMorphAnalyzerMCP.validateToolCallRequest(request));
    }

    @Test
    public void testBuildJsonRpcErrorPreservesStringId() throws Exception {
        JsonNode idNode = MAPPER.readTree("\"request-1\"");

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.buildJsonRpcError(MAPPER, idNode, -32602, "Invalid params: params.arguments.q must be a string"));

        assertEquals("2.0", actual.path("jsonrpc").asText());
        assertEquals("request-1", actual.path("id").asText());
        assertTrue(actual.path("id").isTextual());
        assertEquals(-32602, actual.path("error").path("code").asInt());
        assertEquals("Invalid params: params.arguments.q must be a string", actual.path("error").path("message").asText());
    }

    @Test
    public void testBuildJsonRpcErrorPreservesNumericId() throws Exception {
        JsonNode idNode = MAPPER.readTree("20");

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.buildJsonRpcError(MAPPER, idNode, -32602, "Invalid params: params must be an object"));

        assertEquals(20, actual.path("id").asInt());
        assertTrue(actual.path("id").isInt());
        assertEquals(-32602, actual.path("error").path("code").asInt());
    }

    @Test
    public void testBuildJsonRpcErrorPreservesWhitespaceInMessage() throws Exception {
        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.buildJsonRpcError(MAPPER, MAPPER.readTree("\"request-1\""), -32602, "Invalid params: line 1\nline 2"));

        assertEquals("Invalid params: line 1\nline 2", actual.path("error").path("message").asText());
    }

    @Test
    public void testHandleMcpMessageReturnsInvalidRequestForNonObjectJson() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "[]");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertTrue(actual.path("id").isNull());
        assertEquals(-32600, actual.path("error").path("code").asInt());
        assertEquals("Invalid Request", actual.path("error").path("message").asText());
    }

    @Test
    public void testHandleMcpMessageReturnsInvalidRequestForMissingMethod() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"request-1\"}");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertEquals("request-1", actual.path("id").asText());
        assertEquals(-32600, actual.path("error").path("code").asInt());
        assertEquals("Invalid Request", actual.path("error").path("message").asText());
    }

    @Test
    public void testHandleMcpMessageReturnsMethodNotFoundForUnknownMethod() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":20,\"method\":\"unknown\"}");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertEquals(20, actual.path("id").asInt());
        assertEquals(-32601, actual.path("error").path("code").asInt());
        assertEquals("Method not found", actual.path("error").path("message").asText());
    }

    @Test
    public void testHandleMcpMessageReturnsInvalidParamsForInvalidToolCall() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"bad-1\",\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{}}}");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertEquals("bad-1", actual.path("id").asText());
        assertEquals(-32602, actual.path("error").path("code").asInt());
        assertEquals("Invalid params: params.arguments.q must be a string", actual.path("error").path("message").asText());
    }

    @Test
    public void testHandleMcpMessageReturnsInternalErrorWithRequestIdForToolCallException() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, createThrowingAnalyzer(), "{\"jsonrpc\":\"2.0\",\"id\":\"call-1\",\"method\":\"tools/call\",\"params\":{\"name\":\"jmorph.tokenize\",\"arguments\":{\"q\":\"水車\"}}}");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertEquals("call-1", actual.path("id").asText());
        assertEquals(-32603, actual.path("error").path("code").asInt());
        assertEquals("Internal error", actual.path("error").path("message").asText());
    }

    @Test
    public void testHandleMcpMessageSelectsStructuredContentProtocolVersionForSupportedClient() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"init-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\"}}");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertEquals("init-1", actual.path("id").asText());
        assertEquals("2025-06-18", actual.path("result").path("protocolVersion").asText());
    }

    @Test
    public void testHandleMcpMessageCapsNewerProtocolVersionAtStructuredContentVersion() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"init-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-11-25\"}}");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertEquals("2025-06-18", actual.path("result").path("protocolVersion").asText());
    }

    @Test
    public void testHandleMcpMessageFallsBackToLegacyProtocolVersionForOlderClient() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"init-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\"}}");
            }
        });

        JsonNode actual = MAPPER.readTree(output);
        assertEquals("2024-11-05", actual.path("result").path("protocolVersion").asText());
    }

    @Test
    public void testHandleMcpMessageOmitsOutputSchemaForLegacyProtocolVersion() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"init-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\"}}");
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"list-1\",\"method\":\"tools/list\"}");
            }
        });

        String[] responses = output.split("\\n");
        JsonNode actual = MAPPER.readTree(responses[1]);
        assertFalse(actual.path("result").path("tools").get(0).has("outputSchema"));
    }

    @Test
    public void testHandleMcpMessageIncludesOutputSchemaForStructuredContentProtocolVersion() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"init-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\"}}");
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"id\":\"list-1\",\"method\":\"tools/list\"}");
            }
        });

        String[] responses = output.split("\\n");
        JsonNode actual = MAPPER.readTree(responses[1]);
        String entriesDescription = actual.path("result").path("tools").get(0)
                .path("inputSchema").path("properties").path("context")
                .path("properties").path("entries").path("description").asText();
        JsonNode outputSchema = actual.path("result").path("tools").get(0).path("outputSchema");
        JsonNode tokensSchema = outputSchema.path("properties").path("tokens");

        assertTrue(entriesDescription.contains("`token` or `token,reading`"));
        assertEquals("object", outputSchema.path("type").asText());
        assertEquals("array", tokensSchema.path("type").asText());
        assertEquals("array", tokensSchema.path("items").path("type").asText());
        assertEquals("string", tokensSchema.path("items").path("items").path("type").asText());
        assertTrue(tokensSchema.path("items").path("description").asText().contains("first element is the surface form"));
        assertFalse(outputSchema.path("additionalProperties").asBoolean(true));
    }

    @Test
    public void testHandleMcpMessageDoesNotRespondToInitializedNotification() throws Exception {
        String output = captureStdout(new ThrowingRunnable() {
            public void run() throws Exception {
                JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
            }
        });

        assertEquals("", output);
    }

    @Test
    public void testHandleMcpMessagePropagatesJsonParseError() throws Exception {
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                () -> JMorphAnalyzerMCP.handleMcpMessage(MAPPER, null, "{"));
    }

    @Test
    public void testConvertCallJsonOutputUsesJMorphStringId() throws Exception {
        String jmorphOutput = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"1000\","
                + "\"result\":{\"tokens\":[[\"水車\",\"すいしゃ\",\"水車\",\"品詞1\",\"詳細品詞1\",\"活用型1\",\"活用形1\"]]}"
                + "}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonOutput(MAPPER, jmorphOutput, true));

        assertEquals("2.0", actual.path("jsonrpc").asText());
        assertEquals("1000", actual.path("id").asText());
        assertTrue(actual.path("id").isTextual());
        assertEquals("text", actual.path("result").path("content").get(0).path("type").asText());
        assertEquals("Analyzed 1 tokens.", actual.path("result").path("content").get(0).path("text").asText());
        assertFalse(actual.path("result").path("isError").asBoolean());
        assertEquals("水車", actual.path("result").path("structuredContent").path("tokens").get(0).get(0).asText());
        assertEquals("すいしゃ", actual.path("result").path("structuredContent").path("tokens").get(0).get(1).asText());
    }

    @Test
    public void testConvertCallJsonOutputUsesJMorphNumericId() throws Exception {
        String jmorphOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"tokens\":[]}}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonOutput(MAPPER, jmorphOutput, true));

        assertEquals(1, actual.path("id").asInt());
        assertTrue(actual.path("id").isInt());
    }

    @Test
    public void testConvertCallJsonOutputUsesTextContentOnlyWhenStructuredContentIsDisabled() throws Exception {
        String jmorphOutput = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"1000\","
                + "\"result\":{\"tokens\":[[\"水車\",\"すいしゃ\",\"水車\",\"品詞1\",\"詳細品詞1\",\"活用型1\",\"活用形1\"]]}"
                + "}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonOutput(MAPPER, jmorphOutput, false));
        JsonNode textContent = MAPPER.readTree(actual.path("result").path("content").get(0).path("text").asText());

        assertFalse(actual.path("result").has("structuredContent"));
        assertEquals("水車", textContent.path("tokens").get(0).get(0).asText());
        assertEquals("すいしゃ", textContent.path("tokens").get(0).get(1).asText());
    }

    @Test
    public void testConvertCallJsonOutputConvertsJMorphErrorToMcpToolError() throws Exception {
        String jmorphOutput = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":null,"
                + "\"error\":{\"code\":-32603,\"message\":\"Internal error line 1\\nline 2\"}"
                + "}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonOutput(MAPPER, jmorphOutput, true));
        JsonNode textContent = MAPPER.readTree(actual.path("result").path("content").get(0).path("text").asText());

        assertTrue(actual.path("id").isNull());
        assertTrue(actual.path("result").path("isError").asBoolean());
        assertEquals("text", actual.path("result").path("content").get(0).path("type").asText());
        assertEquals(-32603, textContent.path("error").path("code").asInt());
        assertEquals("Internal error line 1\nline 2", textContent.path("error").path("message").asText());
        assertFalse(actual.path("result").has("structuredContent"));
    }

    @Test
    public void testConvertCallJsonOutputTreatsMissingTokensAsMcpToolError() throws Exception {
        String jmorphOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}";

        JsonNode actual = MAPPER.readTree(JMorphAnalyzerMCP.convertCallJsonOutput(MAPPER, jmorphOutput, true));
        JsonNode textContent = MAPPER.readTree(actual.path("result").path("content").get(0).path("text").asText());

        assertEquals(1, actual.path("id").asInt());
        assertTrue(actual.path("result").path("isError").asBoolean());
        assertEquals(-32603, textContent.path("error").path("code").asInt());
        assertEquals("Invalid JMorph response: missing result.tokens", textContent.path("error").path("message").asText());
        assertFalse(actual.path("result").has("structuredContent"));
    }

    @Test
    public void testReadMcpMessageReadsLineDelimitedJson() throws Exception {
        String input = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        String actual = JMorphAnalyzerMCP.readMcpMessage(in);

        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}", actual);
    }

    @Test
    public void testReadMcpMessageSkipsLeadingEmptyLines() throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";
        String input = "\r\n\n" + body + "\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        String actual = JMorphAnalyzerMCP.readMcpMessage(in);

        assertEquals(body, actual);
    }

    @Test
    public void testReadMcpMessageAcceptsMessageAtMaximumSize() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("12345\n".getBytes(StandardCharsets.UTF_8));

        assertEquals("12345", JMorphAnalyzerMCP.readMcpMessage(in, 5));
    }

    @Test
    public void testReadMcpMessageRejectsMessageOverMaximumSizeWithoutNewline() {
        ByteArrayInputStream in = new ByteArrayInputStream("123456".getBytes(StandardCharsets.UTF_8));

        Exception error = assertThrows(
                java.io.IOException.class,
                () -> JMorphAnalyzerMCP.readMcpMessage(in, 5));

        assertEquals("MCP message exceeds maximum size of 5 bytes", error.getMessage());
    }

    @Test
    public void testReadMcpMessageLimitsUtf8EncodedBytes() {
        ByteArrayInputStream in = new ByteArrayInputStream("水車\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(java.io.IOException.class, () -> JMorphAnalyzerMCP.readMcpMessage(in, 5));
    }

    @Test
    public void testReadMcpMessageCountsCarriageReturnsTowardLimit() {
        ByteArrayInputStream in = new ByteArrayInputStream("\r\r\r\r\r\r".getBytes(StandardCharsets.UTF_8));

        assertThrows(java.io.IOException.class, () -> JMorphAnalyzerMCP.readMcpMessage(in, 5));
    }

    @Test
    public void testResolveMaxMessageBytesUsesDefault() {
        assertEquals(
                JMorphAnalyzerMCP.DEFAULT_MAX_MESSAGE_BYTES,
                JMorphAnalyzerMCP.resolveMaxMessageBytes(null, null));
    }

    @Test
    public void testResolveMaxMessageBytesPrefersSystemProperty() {
        assertEquals(2048, JMorphAnalyzerMCP.resolveMaxMessageBytes("2048", "4096"));
    }

    @Test
    public void testResolveMaxMessageBytesUsesEnvironmentValue() {
        assertEquals(4096, JMorphAnalyzerMCP.resolveMaxMessageBytes(null, "4096"));
    }

    @Test
    public void testResolveMaxMessageBytesRejectsInvalidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JMorphAnalyzerMCP.resolveMaxMessageBytes("unlimited", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> JMorphAnalyzerMCP.resolveMaxMessageBytes("0", null));
    }
}
