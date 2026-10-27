package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
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
        return unsupportedReasons(request).isEmpty();
    }

    /**
     * Lists what the request needs that this model can't do, so a failure can say what ruled the model out. Each
     * reason reads as a continuation of "this model", for example "does not support the TOOLS capability".
     *
     * @return The unmet requirements, empty if the model can handle the request.
     */
    public List<String> unsupportedReasons(ModelRequest request) {
        List<String> reasons = new ArrayList<>();
        Optional<ThinkingLevelContent> thinkingLevel = request.getModelInputs().stream()
                .filter(ThinkingLevelContent.class::isInstance)
                .map(ThinkingLevelContent.class::cast)
                .findFirst();
        if (thinkingLevel.isPresent()
                && resolveThinkingLevel(thinkingLevel.get().getThinkingLevel()).isEmpty()) {
            reasons.add("does not support thinking level " + thinkingLevel.get().getThinkingLevel());
        }
        request.getModelInputs().stream()
                .filter(TemperatureContent.class::isInstance)
                .map(TemperatureContent.class::cast)
                .map(TemperatureContent::getTemperature)
                .filter(temperature -> !supportsTemperature(temperature))
                .forEach(temperature -> reasons.add(temperatureReason(temperature)));
        sortedById(request.getInputTypes()).stream()
                .filter(inputType -> !supportsInput(inputType))
                .forEach(inputType -> reasons.add("does not support the " + inputType + " input type"));
        sortedById(request.getOutputTypes()).stream()
                .filter(outputType -> !supportsOutput(outputType))
                .forEach(outputType -> reasons.add("does not support the " + outputType + " output type"));
        sortedById(request.getRequiredCapabilities()).stream()
                .filter(capability -> !hasCapability(capability))
                .forEach(capability -> reasons.add("does not support the " + capability + " capability"));
        return reasons;
    }

    private String temperatureReason(double temperature) {
        if (temperature < 0) {
            return "temperature " + temperature + " is negative";
        }
        return "temperature " + temperature + " exceeds the model's maximum of " + maxTemperature;
    }

    private static <T> List<T> sortedById(Collection<T> values) {
        return values.stream().sorted(Comparator.comparing(Object::toString)).toList();
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
