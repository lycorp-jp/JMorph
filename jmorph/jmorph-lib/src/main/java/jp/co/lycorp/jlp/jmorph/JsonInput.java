// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class JsonInput {
    private JsonNode id;
    private JsonNode jsonrpc;
    private JsonNode method;
    private JsonNode params;

    public boolean isValidParams() {
        if (params == null || !params.isObject()) {
            return false;
        }
        JsonNode query = params.get("q");
        JsonNode context = params.get("context");
        return query != null && query.isTextual() && isValidContext(context);
    }

    public boolean isValidId() {
        return id != null && (id.isTextual() || id.isNumber() || id.isNull());
    }

    public String getQuery() {
        JsonNode query = (params == null) ? null : params.get("q");
        return query == null || !query.isTextual() ? null : query.asText();
    }

    public List<UserDicEntry> getEntries() {
        return UserDicEntryParser.parse(getEntriesNode());
    }

    private boolean isValidContext(JsonNode context) {
        if (context == null) {
            return true;
        }
        if (!context.isObject()) {
            return false;
        }
        JsonNode entries = context.get("entries");
        if (entries == null) {
            return true;
        }
        if (!entries.isArray()) {
            return false;
        }
        for (JsonNode entry : entries) {
            if (!entry.isTextual()) {
                return false;
            }
        }
        return true;
    }

    private JsonNode getEntriesNode() {
        JsonNode context = (params == null) ? null : params.get("context");
        return (context == null) ? null : context.get("entries");
    }

    public String toString() {
        return "JsonInput [id=" + id + ", jsonrpc=" + jsonrpc + ", method=" + method + ", params=" + params + "]";
    }

    public String getId() {
        return (id == null || id.isNull()) ? null : id.asText();
    }

    public JsonNode getIdNode() {
        return id;
    }

    public String getJsonrpc() {
        return (jsonrpc == null || !jsonrpc.isTextual()) ? null : jsonrpc.asText();
    }

    public String getMethod() {
        return (method == null || !method.isTextual()) ? null : method.asText();
    }

    public JsonNode getParams() {
        return params;
    }
}
