// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AnalyzerConfig.Builder.class)
public final class AnalyzerConfig {
    private static final long DEFAULT_TIMEOUT_MILLIS = 10_000L;  // 10sec

    private final DictionaryConfig dictionary;
    private final int nbestNum;
    private final OutputMode outputMode;
    private final long timeoutMillis;

    private AnalyzerConfig(Builder builder) {
        this.dictionary = builder.dictionary;
        this.nbestNum = (builder.nbestNum != null) ? builder.nbestNum : 1;
        this.outputMode = (builder.outputMode != null) ? builder.outputMode : OutputMode.ALL;
        this.timeoutMillis = (builder.timeoutMillis != null) ? builder.timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    /**
     * Returns a builder for analyzer configuration.
     *
     * @return analyzer configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the dictionary configuration.
     *
     * @return dictionary configuration
     */
    public DictionaryConfig getDictionary() {
        return dictionary;
    }

    /**
     * Returns the N-best number.
     *
     * @return number of N-best results
     */
    public int getNbestNum() {
        return nbestNum;
    }

    /**
     * Returns the output mode.
     *
     * @return output mode
     */
    public OutputMode getOutputMode() {
        return outputMode;
    }

    /**
     * Returns the analysis timeout in milliseconds.
     *
     * @return timeout in milliseconds, or {@code 0} if disabled
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    @Override
    public String toString() {
        return "AnalyzerConfig{"
                + "dictionary=" + dictionary
                + ", nbestNum=" + nbestNum
                + ", outputMode=" + outputMode
                + ", timeoutMillis=" + timeoutMillis
                + "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private DictionaryConfig dictionary;
        private Integer nbestNum;
        private OutputMode outputMode;
        private Long timeoutMillis;

        /**
         * Sets the dictionary configuration.
         *
         * @param dictionary dictionary configuration
         * @return this builder
         */
        public Builder dictionary(DictionaryConfig dictionary) {
            this.dictionary = dictionary;
            return this;
        }

        /**
         * Sets the N-best number.
         *
         * @param nbestNum number of N-best results
         * @return this builder
         */
        public Builder nbestNum(int nbestNum) {
            this.nbestNum = nbestNum;
            return this;
        }

        /**
         * Sets the output mode.
         *
         * @param outputMode output mode
         * @return this builder
         */
        public Builder outputMode(OutputMode outputMode) {
            this.outputMode = outputMode;
            return this;
        }

        /**
         * Sets the analysis timeout in milliseconds. Set to {@code 0} to disable the timeout.
         *
         * @param timeoutMillis timeout in milliseconds
         * @return this builder
         */
        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * Builds an analyzer configuration.
         *
         * @return analyzer configuration
         * @throws IllegalStateException if the dictionary is not set
         */
        public AnalyzerConfig build() {
            if (dictionary == null) {
                throw new IllegalStateException("dictionary is required");
            }
            if (nbestNum != null && nbestNum <= 0) {
                throw new IllegalArgumentException("nbestNum must be greater than 0");
            }
            if (timeoutMillis != null && timeoutMillis < 0) {
                throw new IllegalArgumentException("timeoutMillis must be greater than or equal to 0");
            }
            return new AnalyzerConfig(this);
        }
    }

}
