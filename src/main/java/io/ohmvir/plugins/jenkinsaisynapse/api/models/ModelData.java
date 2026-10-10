package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * What a model can do, as reported by its {@link ModelProvider}. Plain data: it can be built and queried without a
 * running Jenkins.
 */
public class ModelData {
    private @Getter @Setter String modelId;
    private @Getter @Setter List<ModelCapability> capabilities;
    private @Getter @Setter List<ModelInputType> inputs;
    private @Getter @Setter List<ModelOutputType> outputs;
    private @Getter @Setter List<ModelThinkingLevel> supportedThinkingLevels;
    private @Getter @Setter String providerType;
    private @Getter @Setter @Nullable Long maxInputTokens;
    private @Getter @Setter @Nullable Long maxOutputTokens;
    private @Getter @Setter long contextWindow;
    private @Getter @Setter @Nullable Double maxTemperature;

    public ModelData(String modelId, String providerType) {
        this.modelId = modelId;
        this.providerType = providerType;
        this.capabilities = new ArrayList<>();
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.supportedThinkingLevels = new ArrayList<>();
        this.contextWindow = 0L;
    }

    public ModelData(ModelConfiguration config) {
        this(config.getModelId(), config.getProviderType());
    }

    public boolean supportsRequest(ModelRequest request) {
        Optional<ThinkingLevelContent> thinkingLevel = request.getModelInputs().stream()
                .filter(ThinkingLevelContent.class::isInstance)
                .map(ThinkingLevelContent.class::cast)
                .findFirst();
        if (thinkingLevel.isPresent()
                && resolveThinkingLevel(thinkingLevel.get().getThinkingLevel()).isEmpty()) {
            return false;
        }
        if (request.getModelInputs().stream()
                .anyMatch(modelInput -> modelInput instanceof TemperatureContent temperatureContent
                        && !supportsTemperature(temperatureContent.getTemperature()))) {
            return false;
        }
        if (request.getInputTypes().stream().anyMatch(inputType -> !supportsInput(inputType))) {
            return false;
        }
        if (request.getOutputTypes().stream().anyMatch(outputType -> !supportsOutput(outputType))) {
            return false;
        }
        if (request.getRequiredCapabilities().stream().anyMatch(capability -> !hasCapability(capability))) {
            return false;
        }
        return true;
    }

    public boolean hasCapability(ModelCapability capability) {
        return capabilities.contains(capability);
    }

    public boolean supportsInput(ModelInputType input) {
        return inputs.contains(input);
    }

    public boolean supportsOutput(ModelOutputType output) {
        return outputs.contains(output);
    }

    /**
     * Maps a requested thinking level to the nearest one this model supports. Clients should send the resolved level
     * rather than the requested one.
     *
     * @see ModelThinkingLevel#resolveAgainst(Collection)
     */
    public Optional<ModelThinkingLevel> resolveThinkingLevel(ModelThinkingLevel requested) {
        return requested.resolveAgainst(supportedThinkingLevels);
    }

    /**
     * @return true if the requested level, or a nearby one, can be used with this model.
     */
    public boolean supportsThinkingLevel(ModelThinkingLevel input) {
        return resolveThinkingLevel(input).isPresent();
    }

    public boolean supportsTemperature(double temperature) {
        return temperature >= 0 && (maxTemperature == null || temperature <= maxTemperature);
    }

    public boolean supportsThinking() {
        return supportedThinkingLevels.stream().anyMatch(level -> level != ModelThinkingLevel.OFF);
    }
}
