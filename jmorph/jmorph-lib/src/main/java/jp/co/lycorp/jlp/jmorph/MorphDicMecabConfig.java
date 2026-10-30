// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class MorphDicMecabConfig implements MorphDicConfig {
    private static final String TYPE = "mecab";

    private final String location;

    @JsonCreator
    public MorphDicMecabConfig(
            @JsonProperty("location") String location) {
        if (location == null) {
            throw new IllegalArgumentException("location is required");
        }
        this.location = location;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "MorphDicMecabConfig{"
                + "type=" + TYPE
                + ", location=" + location
                + "}";
    }
}
