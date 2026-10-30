// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UnknownDicBasicConfig implements UnknownDicConfig {
    private static final String TYPE = "basic";

    private final String location;
    private final ConcatMode concatMode;
    private final String delim;

    public UnknownDicBasicConfig(ConcatMode concatMode) {
        this(null, concatMode, " ");
    }

    @JsonCreator
    public UnknownDicBasicConfig(
            @JsonProperty("location") String location,
            @JsonProperty("concatMode") ConcatMode concatMode,
            @JsonProperty("delim") String delim) {
        this.location = location;
        this.concatMode = (concatMode != null) ? concatMode : ConcatMode.ALL;
        this.delim = (delim != null) ? delim : " ";
    }

    public static UnknownDicBasicConfig defaultConfig() {
        return new UnknownDicBasicConfig(ConcatMode.ALL);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getLocation() {
        return location;
    }

    public ConcatMode getConcatMode() {
        return concatMode;
    }

    public String getDelim() {
        return delim;
    }

    @Override
    public String toString() {
        return "UnknownDicBasicConfig{"
                + "type=" + TYPE
                + ", location=" + location
                + ", concatMode=" + concatMode
                + ", delim=[" + delim + "]"
                + "}";
    }
}
