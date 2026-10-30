// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = JMorphConfig.Builder.class)
public class JMorphConfig {

    private final String description;
    // hierarchical configuration
    private final AnalyzerConfig analyzer;
    private final JsonrpcConfig jsonrpc;

    private JMorphConfig(Builder builder) {
        this.description = builder.description;
        this.analyzer = builder.analyzer;
        this.jsonrpc = (builder.jsonrpc != null) ? builder.jsonrpc : JsonrpcConfig.defaultConfig();
    }

    /**
     * Returns a builder for JMorph configuration.
     *
     * @return JMorph configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads a configuration from a JSON file.
     *
     * @param configPath path to the configuration JSON file
     * @return loaded configuration
     */
    public static JMorphConfig load(Path configPath) {
        return load(configPath.toString());
    }

    /**
     * Loads a configuration from a JSON file location.
     *
     * @param location path or FileUtils-supported location to the configuration JSON file
     * @return loaded configuration
     */
    public static JMorphConfig load(String location) {
        try {
            try (InputStream is = FileUtils.open(location)) {
                return load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    /**
     * Loads a configuration from a JSON stream.
     *
     * @param is configuration JSON stream
     * @return loaded configuration
     */
    public static JMorphConfig load(InputStream is) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(is, JMorphConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    /**
     * Loads a configuration from a JSON string.
     *
     * @param json configuration JSON string
     * @return loaded configuration
     */
    public static JMorphConfig fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(json, JMorphConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    /**
     * Returns the description of this configuration.
     *
     * @return description of this configuration (may be null)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the analyzer configuration.
     *
     * @return analyzer configuration
     */
    public AnalyzerConfig getAnalyzerConfig() {
        return analyzer;
    }

    /**
     * Returns the jsonrpc configuration.
     *
     * @return jsonrpc configuration
     */
    public JsonrpcConfig getJsonrpcConfig() {
        return jsonrpc;
    }

    @Override
    public String toString() {
        return "JMorphConfig{"
                + "description=" + description
                + ", analyzer=" + analyzer
                + ", jsonrpc=" + jsonrpc
                + "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String description;
        private AnalyzerConfig analyzer;
        private JsonrpcConfig jsonrpc;

        /**
         * Sets the optional human-readable description.
         *
         * @param description description of this configuration
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the analyzer configuration.
         *
         * @param analyzer analyzer configuration
         * @return this builder
         */
        public Builder analyzer(AnalyzerConfig analyzer) {
            this.analyzer = analyzer;
            return this;
        }

        /**
         * Sets the jsonrpc configuration.
         *
         * @param jsonrpc jsonrpc configuration
         * @return this builder
         */
        public Builder jsonrpc(JsonrpcConfig jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Builds a JMorph configuration.
         *
         * @return JMorph configuration
         */
        public JMorphConfig build() {
            if (analyzer == null) {
                throw new IllegalStateException("analyzer is required");
            }
            return new JMorphConfig(this);
        }
    }
}
