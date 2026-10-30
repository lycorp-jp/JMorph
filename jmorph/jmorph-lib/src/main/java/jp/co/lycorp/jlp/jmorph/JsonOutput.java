// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonOutput {

    private JsonNode id;
    private String jsonrpc;
    private Result result;

    public static class Result {

        private String[][] tokens;

        public Result(int size) {
            tokens = new String[size][];
        }

        public void addTokens(int index, String[] token) {
            this.tokens[index] = token;
        }

        public String[][] getTokens() {
            return tokens;
        }
    }

    public void setId(JsonNode id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = (id == null) ? null : TextNode.valueOf(id);
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public Result getResult() {
        return result;
    }

    public JsonNode getId() {
        return id;
    }
}
