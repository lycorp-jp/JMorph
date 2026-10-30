// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import java.io.IOException;
import java.io.InputStream;

public class JsonInputFormatter {
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
            .registerModule(new AfterburnerModule());
    private static final ObjectReader reader = mapper.readerFor(JsonInput.class);


    public static JsonInput translateInput(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        if (root == null || !root.isObject()) {
            return null;
        }
        return reader.readValue(root);
    }

    public static JsonInput getJsonInput(String file) throws IOException {
        try (InputStream in = FileUtils.open(file)) {
            JsonNode json = mapper.readTree(in);
            if (json == null || !json.isObject()) {
                return null;
            }
            return reader.readValue(json);
        }
    }

}
