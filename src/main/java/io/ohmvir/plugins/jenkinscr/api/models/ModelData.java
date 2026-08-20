package io.ohmvir.plugins.jenkinscr.api.models;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.AnthropicModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.GeminiModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.OllamaModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.OpenAIModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Extension
public class ModelData {
    public ModelData(){

    }
    private static final List<ModelDataRetriever<?>> retrievers = List.of(
            new OllamaModelDataRetriever(),
            new AnthropicModelDataRetriever(),
            new GeminiModelDataRetriever(),
            new OpenAIModelDataRetriever()
    );
    private static final Logger LOGGER = Logger.getLogger(ModelData.class.getName());
    private static final HashMap<String, ModelData> MODEL_DATA = new HashMap<>();

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initializeModelDataCache(){
        LOGGER.info("Prefetching model data");
        for(ModelConfiguration config : AgenticCodeReviewSettings.get().getModels()){
            LOGGER.info("Prefetching model data for " + config.getModelId());
            initializeModelDataForConfig(config);
        }
    }

    private static void initializeModelDataForConfig(ModelConfiguration config){
        MODEL_DATA.put(config.getModelId(), retrievers.stream()
                .map(retriever -> retriever.retrieveFromConfigurationGen(config))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow());
    }

    public static ModelData get(String modelId){
        return ModelData.MODEL_DATA.get(modelId);
    }

    private @Getter @Setter List<ModelCapability> capabilities;
    public boolean hasCapability(ModelCapability capability){
        return capabilities.contains(capability);
    }
    private @Getter @Setter List<ModelInputType> inputs;
    public boolean supportsInput(ModelInputType input){
        return inputs.contains(input);
    }
    private @Getter @Setter List<ModelOutputType> outputs;
    public boolean supportsOutput(ModelOutputType output){
        return outputs.contains(output);
    }

    private @Getter @Setter List<ModelThinkingLevel> supportedThinkingLevels;
    public boolean supportsThinkingLevel(ModelThinkingLevel input){
        return supportedThinkingLevels.contains(input);
    }
    public boolean supportsThinking(){ return !supportedThinkingLevels.isEmpty(); }

    private @Getter @Setter ModelProviderType providerType;

    private @Getter @Setter @Nullable Long maxInputTokens;
    private @Getter @Setter @Nullable Long maxOutputTokens;
    private @Getter @Setter long contextWindow;
    private @Getter @Setter @Nullable Double maxTemperature;
}
