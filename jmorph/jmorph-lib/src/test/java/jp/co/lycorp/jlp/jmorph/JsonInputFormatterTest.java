// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class JsonInputFormatterTest {

    @Test
    public void testTranslateInput() throws IOException {
        // setup
        String inputJson = "{\"id\": \"2\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"東京特許許可局は実在しない\" }}";
        String expectedId = "2";

        // exec
        JsonInput input = JsonInputFormatter.translateInput(inputJson);

        // verify
        assertEquals(expectedId, input.getId());
    }

    @Test
    public void testGetJsonInput() throws IOException {
        // setup
        String expectedId = "2";

        // exec
        String inputPath = this.getClass().getClassLoader().getResource("input.json").getPath();
        JsonInput input = JsonInputFormatter.getJsonInput(inputPath);

        // verify
        assertEquals(expectedId, input.getId());
    }

}
