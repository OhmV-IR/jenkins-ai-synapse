package io.ohmvir.plugins.jenkinscr.api.models;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.ohmvir.plugins.jenkinscr.api.client.ModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.api.client.impl.AnthropicModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.impl.GeminiModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.impl.OllamaModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.impl.OpenAIModelClient;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.AnthropicModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.GeminiModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.OllamaModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.OpenAIModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.ModelsManagementLink;
import io.ohmvir.plugins.jenkinscr.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.*;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ModelData {
    private static final List<ModelDataRetriever<?>> retrievers = List.of(
            new OllamaModelDataRetriever(),
            new AnthropicModelDataRetriever(),
            new GeminiModelDataRetriever(),
            new OpenAIModelDataRetriever()
    );
    private static final Logger LOGGER = Logger.getLogger(ModelData.class.getName());
    private static final HashMap<String, ModelData> MODEL_DATA = new HashMap<>();
    private @Getter
    @Setter List<ModelCapability> capabilities;
    private @Getter
    @Setter List<ModelInputType> inputs;
    private @Getter
    @Setter List<ModelOutputType> outputs;
    private @Getter
    @Setter List<ModelThinkingLevel> supportedThinkingLevels;
    private @Getter
    @Setter ModelProviderType providerType;
    private @Getter
    @Setter
    @Nullable Long maxInputTokens;
    private @Getter
    @Setter
    @Nullable Long maxOutputTokens;
    private @Getter
    @Setter long contextWindow;
    private @Getter
    @Setter
    @Nullable Double maxTemperature;

    public ModelData() {

    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initializeModelDataCache() {
        LOGGER.info("Prefetching model data");
        for (ModelConfiguration config : ModelsManagementLink.get().getModelConfigurations()) {
            LOGGER.info("Prefetching model data for " + config.getModelId());
            initializeModelDataForConfig(config);
        }
    }

    public static void initializeModelDataForConfig(ModelConfiguration config) {
        if (MODEL_DATA.containsKey(config.getModelId())) {
            return;
        }
        MODEL_DATA.put(config.getModelId(), retrievers.stream()
                .map(retriever -> retriever.retrieveFromConfigurationGen(config))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow());
    }

    public static ModelData get(String modelId) {
        return ModelData.MODEL_DATA.get(modelId);
    }

    public static ModelClient<?, ?> CreateClient(ModelConfiguration config) {
        return switch (config) {
            case GeminiModelConfiguration geminiConfig ->
                    new GeminiModelClient(get(config.getModelId()), geminiConfig, AgenticCodeReviewSettings.get().getGeminiClientConfiguration());
            case OllamaModelConfiguration ollamaConfig ->
                    new OllamaModelClient(get(config.getModelId()), ollamaConfig, AgenticCodeReviewSettings.get().getOllamaClientConfiguration());
            case OpenAIModelConfiguration openAIConfig ->
                    new OpenAIModelClient(get(config.getModelId()), openAIConfig, AgenticCodeReviewSettings.get().getOpenAIClientConfiguration());
            case AnthropicModelConfiguration anthropicConfig ->
                    new AnthropicModelClient(get(config.getModelId()), anthropicConfig, AgenticCodeReviewSettings.get().getAnthropicClientConfiguration());
            case null, default -> null;
        };
    }

    /**
     * Tries to find a model that will accommodate your request.
     * @param request The model request you want to execute.
     * @return null if no models are capable of fulfilling that request or a model client that can fulfill the request.
     */
    public static @Nullable ModelClient<?, ?> CreateClientForRequest(ModelRequest request){
        return null; // TODO
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

    public boolean supportsThinkingLevel(ModelThinkingLevel input) {
        return supportedThinkingLevels.contains(input);
    }

    public boolean supportsThinking() {
        return !supportedThinkingLevels.isEmpty();
    }
}
