// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDicInRequestProviderBasicConfig implements UserDicInRequestProviderConfig {
    private static final String TYPE = "basic";

    private final String location;
    private final String delim;

    public UserDicInRequestProviderBasicConfig() {
        this(null, " ");
    }

    @JsonCreator
    public UserDicInRequestProviderBasicConfig(
            @JsonProperty("location") String location,
            @JsonProperty("delim") String delim) {
        this.location = location;
        this.delim = (delim != null) ? delim : " ";
    }

    public static UserDicInRequestProviderBasicConfig defaultConfig() {
        return new UserDicInRequestProviderBasicConfig();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getLocation() {
        return location;
    }

    public String getDelim() {
        return delim;
    }

    @Override
    public String toString() {
        return "UserDicInRequestProviderBasicConfig{"
                + "type=" + TYPE
                + ", location=" + location
                + ", delim=[" + delim + "]"
                + "}";
    }
}
