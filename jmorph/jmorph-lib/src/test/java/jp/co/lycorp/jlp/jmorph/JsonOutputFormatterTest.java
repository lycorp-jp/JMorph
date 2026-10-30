// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class JsonOutputFormatterTest {

    @Test
    public void testTranslateJsonOutput() throws IOException {
        // setup
        JsonOutput jsonOutput = new JsonOutput();
        jsonOutput.setId("2");
        jsonOutput.setJsonrpc("2.0");

        JsonOutput.Result result = new JsonOutput.Result(6);
        String[] token0 = {"花子", "品詞1", "詳細品詞1", "はなこ"};
        String[] token1 = {"は", "品詞2", "詳細品詞2", "は"};
        String[] token2 = {"太郎", "品詞1", "詳細品詞1", "たろう"};
        String[] token3 = {"が", "品詞2", "詳細品詞3", "が"};
        String[] token4 = {"好き", "品詞1", "詳細品詞4", "すき"};
        String[] token5 = {"だ", "品詞3", "詳細品詞5", "だ"};
        result.addTokens(0, token0);
        result.addTokens(1, token1);
        result.addTokens(2, token2);
        result.addTokens(3, token3);
        result.addTokens(4, token4);
        result.addTokens(5, token5);
        jsonOutput.setResult(result);

        String expected = "{" + //
                "\"id\":\"2\"," + //
                "\"jsonrpc\":\"2.0\"," + //
                "\"result\":{" + //
                "\"tokens\":[[\"花子\",\"品詞1\",\"詳細品詞1\",\"はなこ\"],[\"は\",\"品詞2\",\"詳細品詞2\",\"は\"],[\"太郎\",\"品詞1\",\"詳細品詞1\",\"たろう\"],[\"が\",\"品詞2\",\"詳細品詞3\",\"が\"],[\"好き\",\"品詞1\",\"詳細品詞4\",\"すき\"],[\"だ\",\"品詞3\",\"詳細品詞5\",\"だ\"]]" + //
                "}" + //
                "}";

        // exec
        String output = JsonOutputFormatter.translate(jsonOutput);

        // verify
        assertEquals(expected, output);
    }

    @Test
    public void testTranslateJsonErrorOutput() throws IOException {
        // setup
        JsonErrorOutput jsonErrorOutput = new JsonErrorOutput();
        jsonErrorOutput.setId("3");
        jsonErrorOutput.setJsonrpc("2.0");

        JsonErrorOutput.JsonError error = new JsonErrorOutput.JsonError();
        error.setCode(-32601);
        error.setMessage("Method not found");
        jsonErrorOutput.setError(error);

        String expected = "{" + //
                "\"id\":\"3\"," + //
                "\"jsonrpc\":\"2.0\"," + //
                "\"error\":{" + //
                "\"code\":-32601," + //
                "\"message\":\"Method not found\"" + //
                "}" + //
                "}";

        // exec
        String output = JsonOutputFormatter.translate(jsonErrorOutput);

        // verify
        assertEquals(expected, output);
    }
}
