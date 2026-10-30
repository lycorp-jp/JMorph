// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = JsonrpcConfig.Builder.class)
public final class JsonrpcConfig {

    private static final String DEFAULT_METHOD = "jmorph.tokenize";
    private static final int DEFAULT_MAX_OBJECT = 16;

    private final String method;
    private final int maxObject;

    private JsonrpcConfig(Builder builder) {
        this.method = (builder.method != null) ? builder.method : DEFAULT_METHOD;
        this.maxObject = (builder.maxObject != null) ? builder.maxObject : DEFAULT_MAX_OBJECT;
    }

    /**
     * Returns a default jsonrpc configuration.
     *
     * @return default jsonrpc configuration
     */
    public static JsonrpcConfig defaultConfig() {
        return builder().build();
    }

    /**
     * Returns a builder for jsonrpc configuration.
     *
     * @return jsonrpc configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the jsonrpc method name.
     *
     * @return method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the maximum number of objects.
     *
     * @return maximum number of objects
     */
    public int getMaxObject() {
        return maxObject;
    }

    @Override
    public String toString() {
        return "JsonrpcConfig{"
                + "method=" + method
                + ", maxObject=" + maxObject
                + "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String method;
        private Integer maxObject;

        /**
         * Sets the jsonrpc method name.
         *
         * @param method jsonrpc method name
         * @return this builder
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the maximum number of objects.
         *
         * @param maxObject maximum number of objects
         * @return this builder
         */
        public Builder maxObject(int maxObject) {
            this.maxObject = maxObject;
            return this;
        }

        /**
         * Builds a jsonrpc configuration.
         *
         * @return jsonrpc configuration
         */
        public JsonrpcConfig build() {
            if (maxObject != null && maxObject <= 0) {
                throw new IllegalArgumentException("maxObject must be greater than 0");
            }
            return new JsonrpcConfig(this);
        }
    }
}
