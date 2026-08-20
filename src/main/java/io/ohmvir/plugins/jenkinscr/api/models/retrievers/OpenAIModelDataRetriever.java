package io.ohmvir.plugins.jenkinscr.api.models.retrievers;

import io.ohmvir.plugins.jenkinscr.api.models.*;
import io.ohmvir.plugins.jenkinscr.configuration.models.OpenAIModelConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenAIModelDataRetriever extends ModelDataRetriever<OpenAIModelConfiguration> {
    public OpenAIModelDataRetriever() {
        super(OpenAIModelConfiguration.class);
    }

    @Override
    public ModelData retrieveFromConfiguration(OpenAIModelConfiguration configuration) throws IOException, InterruptedException {
        // openai api is literally useless and tells us next to nothing about the model
        ModelData ret = new ModelData();
        if(configuration.modelName.contains("gpt-5.5") || configuration.modelName.contains("gpt-5.6")){
            ret.setSupportedThinkingLevels(List.of(ModelThinkingLevel.OFF, ModelThinkingLevel.LOW, ModelThinkingLevel.MEDIUM, ModelThinkingLevel.HIGH, ModelThinkingLevel.EXTRA_HIGH, ModelThinkingLevel.MAX));
        } else if (configuration.modelName.contains("gpt-5.4") || configuration.modelName.contains("gpt-5.3")) {
            ret.setSupportedThinkingLevels(List.of(ModelThinkingLevel.OFF, ModelThinkingLevel.LOW, ModelThinkingLevel.MEDIUM, ModelThinkingLevel.HIGH));
        } else if (configuration.modelName.contains("o3") || configuration.modelName.contains("o4")){
            ret.setSupportedThinkingLevels(List.of(ModelThinkingLevel.LOW, ModelThinkingLevel.MEDIUM, ModelThinkingLevel.HIGH));
        } else {
            ret.setSupportedThinkingLevels(List.of());
        }
        ret.setProviderType(ModelProviderType.OPENAI);
        ret.setMaxTemperature(2.0d);
        ArrayList<ModelCapability> capabilities = new ArrayList<>();
        ArrayList<ModelInputType> inputTypes = new ArrayList<>();
        ArrayList<ModelOutputType> outputTypes = new ArrayList<>();
        outputTypes.add(ModelOutputType.UNSTRUCTURED_TEXT);
        if(configuration.modelName.contains("gpt-5") || configuration.modelName.contains("gpt-4.1") || configuration.modelName.contains("gpt-4o")){
            capabilities.addAll(List.of(ModelCapability.WEB_SEARCH, ModelCapability.STREAMING, ModelCapability.TOOLS, ModelCapability.CODE_EXECUTION, ModelCapability.CITATIONS));
            outputTypes.add(ModelOutputType.STRUCTURED_OUTPUT);
            inputTypes.add(ModelInputType.IMAGE);
            inputTypes.add(ModelInputType.PDF);
        }
        if(configuration.modelName.contains("o3") || configuration.modelName.contains("o4") || configuration.modelName.contains("o1")){
            capabilities.addAll(List.of(ModelCapability.WEB_SEARCH, ModelCapability.STREAMING, ModelCapability.TOOLS, ModelCapability.CODE_EXECUTION, ModelCapability.CITATIONS));
            inputTypes.add(ModelInputType.IMAGE);
            inputTypes.add(ModelInputType.PDF);
            outputTypes.add(ModelOutputType.STRUCTURED_OUTPUT);
        }
        if(configuration.modelName.contains("gpt-4o-audio") || configuration.modelName.contains("-realtime")){
            capabilities.addAll(List.of(ModelCapability.WEB_SEARCH, ModelCapability.STREAMING, ModelCapability.TOOLS, ModelCapability.CODE_EXECUTION, ModelCapability.CITATIONS));
            inputTypes.add(ModelInputType.IMAGE);
            inputTypes.add(ModelInputType.PDF);
            inputTypes.add(ModelInputType.AUDIO);
            outputTypes.add(ModelOutputType.STRUCTURED_OUTPUT);
            outputTypes.add(ModelOutputType.AUDIO);
        }
        if(configuration.modelName.contains("text-embedding-")){
            outputTypes.add(ModelOutputType.EMBEDDINGS);
        }
        if(configuration.modelName.contains("dall-e") || configuration.modelName.contains("sora")){
            inputTypes.add(ModelInputType.IMAGE);
            outputTypes.add(ModelOutputType.VIDEO);
            outputTypes.add(ModelOutputType.IMAGE);
        }
        if(configuration.modelName.contains("whisper")){
            inputTypes.add(ModelInputType.AUDIO);
            capabilities.add(ModelCapability.STREAMING);
        }
        if(configuration.modelName.contains("tts")){
            capabilities.add(ModelCapability.STREAMING);
            outputTypes.add(ModelOutputType.AUDIO);
        }
        ret.setCapabilities(capabilities);
        ret.setOutputs(outputTypes);
        ret.setInputs(inputTypes);
        if(configuration.modelName.contains("gpt-5.6") || configuration.modelName.contains("gpt-5.5")){
            ret.setMaxInputTokens(1_000_000L);
            ret.setContextWindow(1_048_576L);
            ret.setMaxOutputTokens(131_072L);
        }
        else if(configuration.modelName.contains("gpt-5.4") || configuration.modelName.contains("gpt-5.3")){
            ret.setMaxInputTokens(1_000_000L);
            ret.setContextWindow(1_000_000L);
            ret.setMaxOutputTokens(65_000L);
        }
        else if(configuration.modelName.contains("gpt-4.1")){
            ret.setContextWindow(1_047_576L);
            ret.setMaxInputTokens(1_014_808L);
            ret.setMaxOutputTokens(32_768L);
        }
        else if(configuration.modelName.contains("o3") || configuration.modelName.contains("o4")){
            ret.setContextWindow(1_048_576L);
            ret.setMaxInputTokens(1_000_000L);
            ret.setMaxOutputTokens(100_000L);
        }
        else if(configuration.modelName.contains("o1") || configuration.modelName.contains("o3-mini")){
            ret.setContextWindow(200_000L);
            ret.setMaxInputTokens(134_000L);
            ret.setMaxOutputTokens(100_000L);
        }
        else if(configuration.modelName.contains("gpt-4o") || configuration.modelName.contains("gpt-4o-mini")){
            ret.setContextWindow(128_000L);
            ret.setMaxInputTokens(111_616L);
            ret.setMaxOutputTokens(16_384L);
        } else {
            ret.setContextWindow(500_000L);
        }
        return ret;
    }
}
