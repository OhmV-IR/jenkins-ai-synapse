package io.ohmvir.plugins.jenkinscr.api.models;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.ohmvir.plugins.jenkinscr.api.models.retrievers.OllamaModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Extension
public class ModelData {
    private static final List<ModelDataRetriever<?>> retrievers = List.of(
            new OllamaModelDataRetriever()
    );
    private static final Logger LOGGER = Logger.getLogger(ModelData.class.getName());
    private static final ArrayList<ModelData> MODEL_DATA = new ArrayList<>();

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initializeModelDataCache(){
        LOGGER.info("Prefetching model data");
        for(ModelConfiguration config : AgenticCodeReviewSettings.get().getModels()){
            LOGGER.info("Prefetching model data for " + config.getModelId());
            initializeModelDataForConfig(config);
        }
    }

    private static void initializeModelDataForConfig(ModelConfiguration config){
        MODEL_DATA.add(retrievers.stream()
                .map(retriever -> retriever.retrieveFromConfigurationGen(config))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow());
    }

    private @Getter List<ModelCapability> capabilities;
    public boolean hasCapability(ModelCapability capability){
        return capabilities.contains(capability);
    }
    private @Getter List<ModelInputType> inputs;
    public boolean supportsInput(ModelInputType input){
        return inputs.contains(input);
    }
    private @Getter List<ModelOutputType> outputs;
    public boolean supportsOutput(ModelOutputType output){
        return outputs.contains(output);
    }

    private @Getter List<ModelInputType> supportedThinkingLevels;
    public boolean supportsThinkingLevel(ModelInputType input){
        return supportedThinkingLevels.contains(input);
    }

    private @Getter ModelProviderType providerType;

    private @Getter @Nullable Long maxInputTokens;
    private @Getter @Nullable Long maxOutputTokens;
    private @Getter long contextWindow;
    private @Getter @Nullable Long maxTemperature;
// TODO change model configuration temperature validation to account for gemini api maxTemperature and temperatures > 1
}
