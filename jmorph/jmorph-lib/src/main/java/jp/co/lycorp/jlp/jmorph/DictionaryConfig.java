// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = DictionaryConfig.Builder.class)
public class DictionaryConfig {

    private final MorphDicConfig baseDic;
    private final List<MorphDicConfig> additionalDic;
    private final UnknownDicConfig unknownDic;
    private final UserDicInRequestProviderConfig userDicInRequestProvider;

    private DictionaryConfig(Builder builder) {
        this.baseDic = builder.baseDic;
        this.additionalDic = List.copyOf(builder.additionalDic);
        this.unknownDic = (builder.unknownDic != null) ? builder.unknownDic : UnknownDicConfig.defaultConfig();
        this.userDicInRequestProvider = (builder.userDicInRequestProvider != null) ? builder.userDicInRequestProvider
                : UserDicInRequestProviderConfig.defaultConfig();
    }

    /**
     * Returns a builder for dictionary configuration.
     *
     * @return dictionary configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the base dictionary configuration.
     *
     * @return base dictionary configuration
     */
    public MorphDicConfig baseDic() {
        return baseDic;
    }

    /**
     * Returns the additional dictionary configurations.
     *
     * @return additional dictionary configurations
     */
    public List<MorphDicConfig> additionalDic() {
        return additionalDic;
    }

    /**
     * Returns the unknown dictionary configuration.
     *
     * @return unknown dictionary configuration
     */
    public UnknownDicConfig unknownDic() {
        return unknownDic;
    }

    /**
     * Returns the user dictionary in request provider configuration.
     *
     * @return user dictionary in request provider configuration
     */
    public UserDicInRequestProviderConfig userDicInRequestProvider() {
        return userDicInRequestProvider;
    }

    @Override
    public String toString() {
        return "DictionaryConfig{"
                + "baseDic=" + baseDic
                + ", additionalDic=" + additionalDic
                + ", unknownDic=" + unknownDic
                + ", userDicInRequestProvider=" + userDicInRequestProvider
                + "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private MorphDicConfig baseDic = null;
        private final List<MorphDicConfig> additionalDic = new ArrayList<>();
        private UnknownDicConfig unknownDic = null;
        private UserDicInRequestProviderConfig userDicInRequestProvider = null;

        /**
         * Sets the base dictionary configuration.
         *
         * @param config base dictionary configuration
         * @return this builder
         */
        @JsonProperty("baseDic")
        public Builder baseDic(MorphDicConfig config) {
            this.baseDic = config;
            return this;
        }

        /**
         * Adds additional dictionary configurations.
         *
         * @param configs additional dictionary configurations
         * @return this builder
         */
        @JsonProperty("additionalDic")
        public Builder additionalDic(List<MorphDicConfig> configs) {
            this.additionalDic.addAll(configs);
            return this;
        }

        /**
         * Adds an additional dictionary configuration.
         *
         * @param config additional dictionary configuration
         * @return this builder
         */
        @JsonIgnore
        public Builder additionalDic(MorphDicConfig config) {
            this.additionalDic.add(config);
            return this;
        }

        /**
         * Sets the unknown dictionary configuration.
         *
         * @param config unknown dictionary configuration
         * @return this builder
         */
        @JsonProperty("unknownDic")
        public Builder unknownDic(UnknownDicConfig config) {
            this.unknownDic = config;
            return this;
        }

        /**
         * Sets the user dictionary in request provider configuration.
         *
         * @param config user dictionary in request provider configuration
         * @return this builder
         */
        @JsonProperty("userDicInRequestProvider")
        public Builder userDicInRequestProvider(UserDicInRequestProviderConfig config) {
            this.userDicInRequestProvider = config;
            return this;
        }

        /**
         * Builds a dictionary configuration.
         *
         * @return dictionary configuration
         * @throws IllegalStateException if the base dictionary is not set
         */
        public DictionaryConfig build() {
            // 現在はデフォルト辞書がないので、baseDicは必須指定
            if (baseDic == null) {
                throw new IllegalStateException("baseDic is required");
            }
            return new DictionaryConfig(this);
        }

        @Override
        public String toString() {
            return "Builder{"
                    + "baseDic=" + baseDic
                    + ", additionalDic=" + additionalDic
                    + ", unknownDic=" + unknownDic
                    + ", userDicInRequestProvider=" + userDicInRequestProvider
                    + "}";
        }
    }
}
