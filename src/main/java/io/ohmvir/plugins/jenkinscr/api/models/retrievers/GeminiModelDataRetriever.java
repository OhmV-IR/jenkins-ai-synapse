package io.ohmvir.plugins.jenkinscr.api.models.retrievers;

import com.google.genai.Client;
import com.google.genai.types.GetModelConfig;
import com.google.genai.types.Model;
import io.ohmvir.plugins.jenkinscr.api.models.*;
import io.ohmvir.plugins.jenkinscr.configuration.models.GeminiModelConfiguration;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;

import java.util.ArrayList;

public class GeminiModelDataRetriever extends ModelDataRetriever<GeminiModelConfiguration> {
    public GeminiModelDataRetriever() {
        super(GeminiModelConfiguration.class);
    }

    @Override
    public ModelData retrieveFromConfiguration(GeminiModelConfiguration configuration) {
        Client client = Client.builder()
                .apiKey(SecretsUtils.getSecretText(configuration.apiKeyCredentialsId, null))
                .build();
        Model model = client.models.get(configuration.modelName, GetModelConfig.builder().build());
        ModelData ret = new ModelData();
        ret.setProviderType(ModelProviderType.GEMINI);
        ret.setMaxInputTokens(Long.valueOf(model.inputTokenLimit().orElseThrow()));
        ret.setMaxOutputTokens(Long.valueOf(model.outputTokenLimit().orElseThrow()));
        ArrayList<ModelThinkingLevel> thinkingLevels = new ArrayList<>();
        thinkingLevels.add(ModelThinkingLevel.LOW);
        thinkingLevels.add(ModelThinkingLevel.MEDIUM);
        thinkingLevels.add(ModelThinkingLevel.HIGH);
        if(!configuration.modelName.contains("pro") && !configuration.modelName.contains("3.7-flash")){
            thinkingLevels.add(ModelThinkingLevel.OFF); // Gemini calls this "minimal" ref: https://ai.google.dev/gemini-api/docs/generate-content/thinking#:~:text=Table_title%3A%20Thinking%20levels%20(Gemini%203)%20Table_content%3A%20%7C,the%20%22no%20thinking%22%20setting%20for%20most%20queries.
            // Note that for 2.5-x models we will need to translate these qualitative levels into specific token budgets for thinking
        }
        ret.setSupportedThinkingLevels(thinkingLevels);
        ret.setMaxTemperature(Double.valueOf(model.maxTemperature().orElseThrow()));
        ArrayList<ModelCapability> capabilitiesEnumArr = new ArrayList<>();
        capabilitiesEnumArr.add(ModelCapability.STREAMING);
        ArrayList<ModelInputType> inputTypes = new ArrayList<>();
        ArrayList<ModelOutputType> outputTypes = new ArrayList<>();
        if(configuration.modelName.contains("-image")){
            inputTypes.add(ModelInputType.TEXT);
            outputTypes.add(ModelOutputType.IMAGE);
            outputTypes.add(ModelOutputType.VIDEO);
        }
        else if(configuration.modelName.contains("-live") || configuration.modelName.contains("-audio")){
            inputTypes.add(ModelInputType.TEXT);
            outputTypes.add(ModelOutputType.AUDIO);
            inputTypes.add(ModelInputType.AUDIO);
        } else if (configuration.modelName.contains("embedding")){
            inputTypes.add(ModelInputType.TEXT);
            outputTypes.add(ModelOutputType.EMBEDDINGS);
        } else {
            inputTypes.add(ModelInputType.TEXT);
            outputTypes.add(ModelOutputType.UNSTRUCTURED_TEXT);
            outputTypes.add(ModelOutputType.STRUCTURED_OUTPUT);
            capabilitiesEnumArr.add(ModelCapability.TOOLS);
            capabilitiesEnumArr.add(ModelCapability.WEB_SEARCH);
            capabilitiesEnumArr.add(ModelCapability.CODE_EXECUTION);
            capabilitiesEnumArr.add(ModelCapability.CITATIONS);
        }
        ret.setCapabilities(capabilitiesEnumArr);
        ret.setInputs(inputTypes);
        ret.setOutputs(outputTypes);
        return ret;
    }
}
