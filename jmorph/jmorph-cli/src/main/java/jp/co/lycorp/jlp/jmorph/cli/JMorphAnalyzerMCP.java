// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jp.co.lycorp.jlp.jmorph.JMorphAnalyzerJsonrpc;
import jp.co.lycorp.jlp.jmorph.JMorphConfig;
import jp.co.lycorp.jlp.jmorph.TokenizerForJsonrpc;

public class JMorphAnalyzerMCP {
    private static final String LEGACY_PROTOCOL_VERSION = "2024-11-05";
    private static final String STRUCTURED_CONTENT_PROTOCOL_VERSION = "2025-06-18";
    static final int DEFAULT_MAX_MESSAGE_BYTES = 1024 * 1024;
    private static final String MAX_MESSAGE_BYTES_PROPERTY = "jmorph.mcp.maxMessageBytes";
    private static final String MAX_MESSAGE_BYTES_ENV = "JMORPH_MCP_MAX_MESSAGE_BYTES";
    private static String currentProtocolVersion = LEGACY_PROTOCOL_VERSION;

    public static void main(String[] args) {
        if (args == null || args.length == 0 || args[0] == null || args[0].isEmpty()) {
            System.err.println("Usage: java jp.co.lycorp.jlp.jmorph.cli.JMorphAnalyzerMCP <init-json-path>");
            return;
        }
        String initArgsPath = args[0];

        ObjectMapper mapper = new ObjectMapper();
        JMorphAnalyzerJsonrpc analyzer;
        int maxMessageBytes;
        try {
            maxMessageBytes = getMaxMessageBytes();
            JMorphConfig config = JMorphConfig.load(initArgsPath);
            analyzer = new JMorphAnalyzerJsonrpc(config);
        } catch (IOException | RuntimeException e) {
            System.err.println(e);
            return;
        }

        System.err.println("JMorph MCP Server Started. Waiting for input...");

        try {
            String message;

            while ((message = readMcpMessage(System.in, maxMessageBytes)) != null) {
                try {
                    handleMcpMessage(mapper, analyzer, message);
                } catch (JsonProcessingException e) {
                    sendResponse(buildJsonRpcError(mapper, NullNode.getInstance(), -32700, "Parse error"));
                } catch (Exception e) {
                    logError("Error while handling MCP message", e);
                    sendResponse(buildJsonRpcError(mapper, NullNode.getInstance(), -32603, "Internal error"));
                }
            }
        } catch (McpMessageTooLargeException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            logError("Error in MCP Server Loop", e);
        }
        
        System.err.println("JMorph MCP Server Stopped.");
    }

    static void handleMcpMessage(ObjectMapper mapper, TokenizerForJsonrpc analyzer, String line) throws Exception {
        logDebug("Received: " + truncateForLog(line));

        JsonNode request = mapper.readTree(line);
        if (!request.isObject()) {
            sendResponse(buildJsonRpcError(mapper, NullNode.getInstance(), -32600, "Invalid Request"));
            return;
        }

        JsonNode idNode = request.get("id");
        if (idNode == null) {
            JsonNode methodNode = request.path("method");
            if (methodNode.isTextual() && "notifications/initialized".equals(methodNode.asText())) {
                logDebug("Initialized notification received.");
            }
            return;
        }

        if (!isValidRequestId(idNode)) {
            sendResponse(buildJsonRpcError(mapper, NullNode.getInstance(), -32600, "Invalid Request"));
            return;
        }

        JsonNode responseIdNode = idNode;
        JsonNode jsonrpcNode = request.path("jsonrpc");
        if (!jsonrpcNode.isTextual() || !"2.0".equals(jsonrpcNode.asText())) {
            sendResponse(buildJsonRpcError(mapper, responseIdNode, -32600, "Invalid Request"));
            return;
        }

        JsonNode methodNode = request.path("method");
        if (!methodNode.isTextual()) {
            sendResponse(buildJsonRpcError(mapper, responseIdNode, -32600, "Invalid Request"));
            return;
        }

        String method = methodNode.asText();
        boolean structuredContentEnabled = STRUCTURED_CONTENT_PROTOCOL_VERSION.equals(currentProtocolVersion);
        if ("initialize".equals(method)) {
            String protocolVersion = selectProtocolVersion(request);
            currentProtocolVersion = protocolVersion;
            sendResponse(buildInitializeResponse(mapper, responseIdNode, protocolVersion));
        } else if ("tools/list".equals(method)) {
            sendResponse(buildToolsListResponse(mapper, responseIdNode, structuredContentEnabled));
        } else if ("tools/call".equals(method)) {
            String validationError = validateToolCallRequest(request);
            if (validationError != null) {
                sendResponse(buildJsonRpcError(mapper, responseIdNode, -32602, validationError));
                return;
            }
            try {
                String inputConverted = convertCallJsonInput(mapper, request);
                logDebug("Converted input: " + truncateForLog(inputConverted));
                String result = analyzer.analyze(inputConverted);
                logDebug("JMorph result: " + truncateForLog(result));
                String response = convertCallJsonOutput(mapper, result, structuredContentEnabled);
                sendResponse(response);
            } catch (Exception e) {
                logError("Error while handling tools/call", e);
                sendResponse(buildJsonRpcError(mapper, responseIdNode, -32603, "Internal error"));
            }
        } else {
            sendResponse(buildJsonRpcError(mapper, responseIdNode, -32601, "Method not found"));
        }
    }

    private static boolean isValidRequestId(JsonNode idNode) {
        return idNode.isTextual() || idNode.isNumber();
    }

    static boolean isDebugEnabled() {
        return Boolean.getBoolean("jmorph.mcp.debug")
                || "true".equalsIgnoreCase(System.getenv("JMORPH_MCP_DEBUG"));
    }

    private static void logDebug(String message) {
        if (isDebugEnabled()) {
            System.err.println(message);
        }
    }

    private static void logError(String message, Exception e) {
        System.err.println(message + ": " + e.getClass().getSimpleName());
        if (isDebugEnabled()) {
            e.printStackTrace(System.err);
        }
    }

    static String truncateForLog(String value) {
        int maxLength = 2000;
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }

    static String readMcpMessage(InputStream in) throws IOException {
        return readMcpMessage(in, DEFAULT_MAX_MESSAGE_BYTES);
    }

    static String readMcpMessage(InputStream in, int maxMessageBytes) throws IOException {
        if (maxMessageBytes <= 0) {
            throw new IllegalArgumentException("Maximum MCP message size must be greater than zero");
        }

        String firstLine;
        do {
            firstLine = readUtf8Line(in, maxMessageBytes);
            if (firstLine == null) return null;
        } while (firstLine.isEmpty());

        return firstLine.trim();
    }

    private static String readUtf8Line(InputStream in, int maxMessageBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        long bytesRead = 0;

        while (true) {
            int b = in.read();
            if (b == -1) {
                return buffer.size() == 0 ? null : buffer.toString(StandardCharsets.UTF_8);
            }
            if (b == '\n') {
                break;
            }
            bytesRead++;
            if (bytesRead > maxMessageBytes) {
                throw new McpMessageTooLargeException(maxMessageBytes);
            }
            if (b != '\r') {
                buffer.write(b);
            }
        }

        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static class McpMessageTooLargeException extends IOException {
        McpMessageTooLargeException(int maxMessageBytes) {
            super("MCP message exceeds maximum size of " + maxMessageBytes + " bytes");
        }
    }

    static int resolveMaxMessageBytes(String systemPropertyValue, String environmentValue) {
        String configuredValue = systemPropertyValue;
        if (configuredValue == null || configuredValue.trim().isEmpty()) {
            configuredValue = environmentValue;
        }
        if (configuredValue == null || configuredValue.trim().isEmpty()) {
            return DEFAULT_MAX_MESSAGE_BYTES;
        }

        try {
            int value = Integer.parseInt(configuredValue.trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Maximum MCP message size must be a positive integer number of bytes: " + configuredValue);
        }
    }

    private static int getMaxMessageBytes() {
        return resolveMaxMessageBytes(
                System.getProperty(MAX_MESSAGE_BYTES_PROPERTY),
                System.getenv(MAX_MESSAGE_BYTES_ENV));
    }

    private static void sendResponse(String jsonResponse) throws IOException {
        String cleanJson = jsonResponse.trim();

        System.out.write((cleanJson + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.flush();
        logDebug("Sent: " + truncateForLog(cleanJson));
    }

    static String validateToolCallRequest(JsonNode request) {
        JsonNode params = request.path("params");
        if (!params.isObject()) {
            return "Invalid params: params must be an object";
        }

        JsonNode name = params.path("name");
        if (!name.isTextual()) {
            return "Invalid params: params.name must be a string";
        }
        if (!"jmorph.tokenize".equals(name.asText())) {
            return "Invalid params: unsupported tool name: " + name.asText();
        }

        JsonNode arguments = params.path("arguments");
        if (!arguments.isObject()) {
            return "Invalid params: params.arguments must be an object";
        }

        JsonNode query = arguments.path("q");
        if (!query.isTextual()) {
            return "Invalid params: params.arguments.q must be a string";
        }

        JsonNode context = arguments.path("context");
        if (!context.isMissingNode() && !context.isNull()) {
            if (!context.isObject()) {
                return "Invalid params: params.arguments.context must be an object";
            }

            JsonNode entries = context.path("entries");
            if (!entries.isMissingNode() && !entries.isNull()) {
                if (!entries.isArray()) {
                    return "Invalid params: params.arguments.context.entries must be an array";
                }
                for (int i = 0; i < entries.size(); i++) {
                    if (!entries.get(i).isTextual()) {
                        return "Invalid params: params.arguments.context.entries[" + i + "] must be a string";
                    }
                }
            }
        }

        return null;
    }

    static String selectProtocolVersion(JsonNode request) {
        String requestedVersion = request.path("params").path("protocolVersion").asText("");
        if (requestedVersion.matches("\\d{4}-\\d{2}-\\d{2}")
                && requestedVersion.compareTo(STRUCTURED_CONTENT_PROTOCOL_VERSION) >= 0) {
            return STRUCTURED_CONTENT_PROTOCOL_VERSION;
        }
        return LEGACY_PROTOCOL_VERSION;
    }

    static String buildInitializeResponse(ObjectMapper mapper, JsonNode idNode, String protocolVersion)
            throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", idNode);

        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", protocolVersion);

        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.set("tools", mapper.createObjectNode());
        result.set("capabilities", capabilities);

        ObjectNode serverInfo = mapper.createObjectNode();
        serverInfo.put("name", "jmorph-mcp");
        serverInfo.put("version", "1.0.0");
        result.set("serverInfo", serverInfo);

        response.set("result", result);
        return mapper.writeValueAsString(response);
    }

    static String buildToolsListResponse(ObjectMapper mapper, JsonNode idNode, boolean structuredContentEnabled)
            throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", idNode);

        ObjectNode tool = mapper.createObjectNode();
        tool.put("name", "jmorph.tokenize");
        tool.put("title", "JMorph Japanese Morphological Analyzer");
        tool.put("description", "Analyze a given string morphologically");
        tool.set("inputSchema", buildInputSchema(mapper));
        if (structuredContentEnabled) {
            tool.set("outputSchema", buildOutputSchema(mapper));
        }

        ObjectNode result = mapper.createObjectNode();
        result.putArray("tools").add(tool);
        response.set("result", result);

        return mapper.writeValueAsString(response);
    }

    private static ObjectNode buildInputSchema(ObjectMapper mapper) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();

        ObjectNode query = mapper.createObjectNode();
        query.put("type", "string");
        query.put("description", "the string to analyze");
        properties.set("q", query);

        ObjectNode context = mapper.createObjectNode();
        context.put("type", "object");
        context.put("description", "optional analysis context including user dictionary entries");

        ObjectNode contextProperties = mapper.createObjectNode();
        ObjectNode entries = mapper.createObjectNode();
        entries.put("type", "array");
        entries.put("description", "Request-scoped user dictionary entries. Each entry is a CSV string in one of these formats: `token` or `token,reading`. `token` is the surface form to force as one token. `reading` is the reading for the token; if omitted, the token itself is used. Example: `水車小屋,すいしゃごや` registers `水車小屋` with reading `すいしゃごや`.");
        entries.set("items", mapper.createObjectNode().put("type", "string"));
        contextProperties.set("entries", entries);
        context.set("properties", contextProperties);
        properties.set("context", context);

        schema.set("properties", properties);
        schema.putArray("required").add("q");
        return schema;
    }

    private static ObjectNode buildOutputSchema(ObjectMapper mapper) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode tokenFields = mapper.createObjectNode();
        tokenFields.put("type", "array");
        tokenFields.put("description",
                "A token field array. The first element is the surface form; subsequent elements depend on the dictionary and output settings.");
        tokenFields.set("items", mapper.createObjectNode().put("type", "string"));

        ObjectNode tokens = mapper.createObjectNode();
        tokens.put("type", "array");
        tokens.put("description", "JMorph tokens. Each token is an array of string fields returned by JMorph.");
        tokens.set("items", tokenFields);

        ObjectNode properties = mapper.createObjectNode();
        properties.set("tokens", tokens);
        schema.set("properties", properties);
        schema.putArray("required").add("tokens");
        schema.put("additionalProperties", false);
        return schema;
    }

    static String buildJsonRpcError(ObjectMapper mapper, JsonNode idNode, int code, String message) throws IOException {
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);

        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", idNode);
        response.set("error", error);

        return mapper.writeValueAsString(response);
    }

    static String convertCallJsonInput(ObjectMapper mapper, JsonNode root) throws Exception {
        ObjectNode output = mapper.createObjectNode();

        output.set("jsonrpc", root.path("jsonrpc"));
        output.set("id", root.path("id"));
        output.set("method", root.path("params").path("name"));
        output.set("params", root.path("params").path("arguments"));

        return mapper.writeValueAsString(output);
    }

    static String convertCallJsonOutput(ObjectMapper mapper, String inputJson, boolean structuredContentEnabled)
            throws Exception {
        JsonNode root = mapper.readTree(inputJson);

        ObjectNode textContent = mapper.createObjectNode();
        textContent.put("type", "text");

        ObjectNode result = mapper.createObjectNode();
        result.putArray("content").add(textContent);

        JsonNode errorNode = root.path("error");
        JsonNode tokensNode = root.path("result").path("tokens");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            ObjectNode error = mapper.createObjectNode();
            error.set("error", errorNode);
            textContent.put("text", mapper.writeValueAsString(error));
            result.put("isError", true);
        } else if (tokensNode.isMissingNode() || tokensNode.isNull()) {
            ObjectNode error = mapper.createObjectNode();
            ObjectNode errorDetail = mapper.createObjectNode();
            errorDetail.put("code", -32603);
            errorDetail.put("message", "Invalid JMorph response: missing result.tokens");
            error.set("error", errorDetail);
            textContent.put("text", mapper.writeValueAsString(error));
            result.put("isError", true);
        } else {
            ObjectNode tokens = mapper.createObjectNode();
            tokens.set("tokens", tokensNode);
            if (structuredContentEnabled) {
                textContent.put("text", "Analyzed " + tokensNode.size() + " tokens.");
                result.set("structuredContent", tokens);
            } else {
                textContent.put("text", mapper.writeValueAsString(tokens));
            }
            result.put("isError", false);
        }

        ObjectNode output = mapper.createObjectNode();
        output.set("jsonrpc", root.path("jsonrpc"));
        output.set("id", root.path("id"));
        output.set("result", result);

        return mapper.writeValueAsString(output);
    }
}
