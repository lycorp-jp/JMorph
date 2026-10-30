// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonErrorOutput {
    private JsonNode id;
    private String jsonrpc;
    private JsonError error;

    public static class JsonError {
        private int code;
        private String message;

        public void setCode(int code) {
            this.code = code;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public void setId(JsonNode id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = (id == null) ? null : TextNode.valueOf(id);
    }

    public void setError(JsonError error) {
        this.error = error;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public JsonNode getId() {
        return id;
    }

    public JsonError getError() {
        return error;
    }
}
