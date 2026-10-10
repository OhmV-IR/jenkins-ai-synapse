package io.ohmvir.plugins.jenkinsaisynapse.api.request;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Settings controlling how the model generates its response. Unset settings use the model's defaults.
 * Instances are immutable; create them with {@link #builder()}.
 */
public final class GenerationParams {
    public static final GenerationParams DEFAULTS = builder().build();

    private final @Nullable Double temperature;
    private final @Nullable Double topP;
    private final @Nullable Integer topK;
    private final @Nullable Integer maxOutputTokens;
    private final List<String> stopSequences;
    private final @Nullable ModelThinkingLevel thinkingLevel;

    private GenerationParams(Builder builder) {
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.topK = builder.topK;
        this.maxOutputTokens = builder.maxOutputTokens;
        this.stopSequences = List.copyOf(builder.stopSequences);
        this.thinkingLevel = builder.thinkingLevel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .temperature(temperature)
                .topP(topP)
                .topK(topK)
                .maxOutputTokens(maxOutputTokens)
                .stopSequences(stopSequences)
                .thinkingLevel(thinkingLevel);
    }

    public @Nullable Double getTemperature() {
        return temperature;
    }

    public @Nullable Double getTopP() {
        return topP;
    }

    public @Nullable Integer getTopK() {
        return topK;
    }

    public @Nullable Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public List<String> getStopSequences() {
        return stopSequences;
    }

    public @Nullable ModelThinkingLevel getThinkingLevel() {
        return thinkingLevel;
    }

    /**
     * @return The capabilities a model needs to honour these settings.
     */
    public Set<ModelCapability> getRequiredCapabilities() {
        Set<ModelCapability> capabilities = EnumSet.noneOf(ModelCapability.class);
        if (temperature != null) {
            capabilities.add(ModelCapability.CUSTOM_TEMPERATURE);
        }
        if (topP != null) {
            capabilities.add(ModelCapability.CUSTOM_TOP_P);
        }
        if (topK != null) {
            capabilities.add(ModelCapability.CUSTOM_TOP_K);
        }
        if (maxOutputTokens != null) {
            capabilities.add(ModelCapability.OUTPUT_TOKEN_LIMITING);
        }
        if (!stopSequences.isEmpty()) {
            capabilities.add(ModelCapability.CUSTOM_STOP_SEQUENCES);
        }
        if (thinkingLevel != null) {
            capabilities.add(ModelCapability.THINKING);
        }
        return capabilities;
    }

    public static final class Builder {
        private @Nullable Double temperature;
        private @Nullable Double topP;
        private @Nullable Integer topK;
        private @Nullable Integer maxOutputTokens;
        private List<String> stopSequences = List.of();
        private @Nullable ModelThinkingLevel thinkingLevel;

        private Builder() {}

        public Builder temperature(@Nullable Double temperature) {
            if (temperature != null && temperature < 0) {
                throw new IllegalArgumentException("temperature must not be negative");
            }
            this.temperature = temperature;
            return this;
        }

        public Builder topP(@Nullable Double topP) {
            if (topP != null && (topP < 0 || topP > 1)) {
                throw new IllegalArgumentException("topP must be between 0 and 1");
            }
            this.topP = topP;
            return this;
        }

        public Builder topK(@Nullable Integer topK) {
            if (topK != null && topK <= 0) {
                throw new IllegalArgumentException("topK must be positive");
            }
            this.topK = topK;
            return this;
        }

        public Builder maxOutputTokens(@Nullable Integer maxOutputTokens) {
            if (maxOutputTokens != null && maxOutputTokens <= 0) {
                throw new IllegalArgumentException("maxOutputTokens must be positive");
            }
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        public Builder stopSequences(@Nullable List<String> stopSequences) {
            this.stopSequences = stopSequences == null ? List.of() : List.copyOf(stopSequences);
            return this;
        }

        public Builder thinkingLevel(@Nullable ModelThinkingLevel thinkingLevel) {
            this.thinkingLevel = thinkingLevel;
            return this;
        }

        public GenerationParams build() {
            return new GenerationParams(this);
        }
    }
}
