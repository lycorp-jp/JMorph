// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class JMorphAnalyzerJsonrpc implements TokenizerForJsonrpc {

    private final JMorphConfig config;
    private final ObjectPool<JMorphAnalyzer> pool;

    /**
     * Creates an analyzer with the given configuration and object pool settings.
     *
     * @param config configuration to use
     * @throws IOException if analyzer initialization fails
     */
    public JMorphAnalyzerJsonrpc(JMorphConfig config) throws IOException {
        this.config = config;
        this.pool = new GenericObjectPool<>(new JMorphAnalyzerPoolFactory(config));
        int maxObj = this.config.getJsonrpcConfig().getMaxObject();
        ((GenericObjectPool<JMorphAnalyzer>) this.pool).setMaxTotal(maxObj);
        // Idleなオブジェクトを捨てたくないので全オブジェクト数と同じ値に設定
        ((GenericObjectPool<JMorphAnalyzer>) this.pool).setMaxIdle(maxObj);
        ((GenericObjectPool<JMorphAnalyzer>) this.pool).setTestOnBorrow(true);
    }

    /**
     * Analyzes the given JSON-RPC request and returns a JSON-RPC response.
     *
     * @param jsonInput JSON-RPC request body
     * @return JSON-RPC response
     */
    public String analyze(String jsonInput) {
        JsonInput input;
        try {
            input = JsonInputFormatter.translateInput(jsonInput);
        } catch (IOException | RuntimeException e) {
            return getJsonErrorOutput(JMorphJsonrpcError.PARSE_ERROR, null);
        }
        return analyze(input);
    }

    /**
     * Analyzes the JSON-RPC request in the given file and returns a JSON-RPC response.
     *
     * @param jsonFile path or FileUtils-supported location of a JSON-RPC request file
     * @return JSON-RPC response
     */
    public String analyzeFile(String jsonFile) {
        JsonInput input;
        try {
            input = JsonInputFormatter.getJsonInput(jsonFile);
        } catch (IOException | RuntimeException e) {
            return getJsonErrorOutput(JMorphJsonrpcError.PARSE_ERROR, null);
        }
        return analyze(input);
    }

    private String analyze(JsonInput input) {
        JMorphAnalyzer engine;

        // 入力のバリデーションを行う
        String validateResult = validateInput(input);

        // バリデーション結果に問題がある場合、エラー結果を返す
        if (validateResult != null) {
            return validateResult;
        }

        boolean shouldInvalidateObject = false;
        boolean userDicEntriesAdded = false;
        String result;
        try {
            engine = (JMorphAnalyzer) this.pool.borrowObject();
            try {
                // 解析
                List<UserDicEntry> userDicEntries = getUserDicEntries(input);

                if (!userDicEntries.isEmpty()) {
                    engine.addUserDicInRequest(userDicEntries);
                    userDicEntriesAdded = true;
                }

                List<TokenInfo> rl = engine.analyze(getQuery(input));

                result = getJsonOutputN1(rl, input.getIdNode());

            } catch (Exception e) {
                shouldInvalidateObject = true;
                result = getJsonErrorOutput(JMorphJsonrpcError.INTERNAL_ERROR, input.getIdNode());
            } finally {
                if (userDicEntriesAdded) {
                    try {
                        engine.removeUserDicInRequest();
                    } catch (Exception e) {
                        shouldInvalidateObject = true;
                        result = getJsonErrorOutput(JMorphJsonrpcError.INTERNAL_ERROR, input.getIdNode());
                    }
                }
                if (shouldInvalidateObject) {
                    this.pool.invalidateObject(engine);
                } else {
                    this.pool.returnObject(engine);
                }
            }
            return result;
        } catch (Exception e) {
            return getJsonErrorOutput(JMorphJsonrpcError.INTERNAL_ERROR, input.getIdNode());
        }
    }

    private byte[] getQuery(JsonInput input) {
        return ByteUtils.toBytes(input.getQuery());
    }

    private List<UserDicEntry> getUserDicEntries(JsonInput input) {
        return input.getEntries();
    }

    private String getJsonOutputN1(List<TokenInfo> resultList, JsonNode id) throws JsonProcessingException {
        JsonOutput output = new JsonOutput();

        output.setId(id);
        output.setJsonrpc("2.0");

        int outputSize = resultList.size() - 1;
        if (resultList.isEmpty()) {
            outputSize = 0;
        }

        JsonOutput.Result outputResult = new JsonOutput.Result(outputSize);
        int i = 0;
        for (TokenInfo result : resultList) {
            byte[][] res = result.getBytes();
            if (i == resultList.size() - 1 && ByteUtils.toString(res[0]).equals("EOS")) {
                break;
            }
            String[] tokens = new String[res.length];
            for (int j = 0; j < tokens.length; ++j) {
                tokens[j] = ByteUtils.toString(res[j]);
            }
            outputResult.addTokens(i++, tokens);
        }

        output.setResult(outputResult);
        return JsonOutputFormatter.translate(output);
    }

    private enum JMorphJsonrpcError {
        PARSE_ERROR(-32700, "Parse error"), INVALID_REQUEST(-32600, "Invalid request"), METHOD_NOT_FOUND(-32601,
                "Method not found"),
        INVALID_PARAMS(-32602, "Invalid params"), INTERNAL_ERROR(-32603,
                "Internal error"),
        SERVER_ERROR(-32000, "Server error");

        private final int code;
        private final String message;

        JMorphJsonrpcError(int code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    private String getJsonErrorOutput(JMorphJsonrpcError e, JsonNode id) {
        JsonErrorOutput output = new JsonErrorOutput();

        output.setId(id);
        output.setJsonrpc("2.0");

        JsonErrorOutput.JsonError error = new JsonErrorOutput.JsonError();
        error.setCode(e.code);
        error.setMessage(e.message);

        output.setError(error);

        try {
            return JsonOutputFormatter.translate(output);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String validateInput(JsonInput input) {
        JMorphJsonrpcError error;
        // 必須フィールドをチェック
        if (input == null) {
            error = JMorphJsonrpcError.INVALID_REQUEST;
            return getJsonErrorOutput(error, null);
        }

        if (!input.isValidId()) {
            error = JMorphJsonrpcError.INVALID_REQUEST;
            return getJsonErrorOutput(error, null);
        }

        if (input.getJsonrpc() == null || input.getMethod() == null) {
            error = JMorphJsonrpcError.INVALID_REQUEST;
            return getJsonErrorOutput(error, input.getIdNode());
        }

        // json rpc のバージョンチェック
        if (!input.getJsonrpc().equals("2.0")) {
            error = JMorphJsonrpcError.INVALID_REQUEST;
            return getJsonErrorOutput(error, input.getIdNode());
        }

        // methodチェック
        if (!input.getMethod().equals(config.getJsonrpcConfig().getMethod())) {
            error = JMorphJsonrpcError.METHOD_NOT_FOUND;
            return getJsonErrorOutput(error, input.getIdNode());
        }

        if (!input.isValidParams()) {
            error = JMorphJsonrpcError.INVALID_PARAMS;
            return getJsonErrorOutput(error, input.getIdNode());
        }

        return null;
    }
}
