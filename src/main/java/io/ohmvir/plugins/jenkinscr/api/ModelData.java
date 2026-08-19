package io.ohmvir.plugins.jenkinscr.api;

import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.List;

public class ModelData {
    // TODO
    public static ModelData createFromConfiguration(ModelConfiguration config){
        return null;
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
