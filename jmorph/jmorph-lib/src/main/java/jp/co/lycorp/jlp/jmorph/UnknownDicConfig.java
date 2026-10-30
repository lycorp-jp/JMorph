// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UnknownDicBasicConfig.class, name = "basic")
})
public interface UnknownDicConfig {
    static UnknownDicConfig defaultConfig() {
        return UnknownDicBasicConfig.defaultConfig();
    }

    String getType();
}
