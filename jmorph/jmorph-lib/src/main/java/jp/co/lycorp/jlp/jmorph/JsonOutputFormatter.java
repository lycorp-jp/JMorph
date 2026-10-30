// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

public class JsonOutputFormatter {
    private static final ObjectMapper mapper =
            new ObjectMapper().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                    .registerModule(new AfterburnerModule());
    private static final ObjectWriter writer = mapper.writer();

    public static String translate(JsonOutput output) throws JsonProcessingException {
        return writer.writeValueAsString(output);
    }

    public static String translate(JsonErrorOutput output) throws JsonProcessingException {
        return writer.writeValueAsString(output);
    }
}
