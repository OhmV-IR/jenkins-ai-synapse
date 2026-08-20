package io.ohmvir.plugins.jenkinscr.api.models.retrievers;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.models.ModelInfo;
import io.ohmvir.plugins.jenkinscr.api.models.*;
import io.ohmvir.plugins.jenkinscr.configuration.models.AnthropicModelConfiguration;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;

import java.io.IOException;
import java.util.ArrayList;

public class AnthropicModelDataRetriever extends ModelDataRetriever<AnthropicModelConfiguration> {
    public AnthropicModelDataRetriever() {
        super(AnthropicModelConfiguration.class);
    }

    @Override
    public ModelData retrieveFromConfiguration(AnthropicModelConfiguration configuration) throws IOException {
        ModelData ret = new ModelData();
        AnthropicClient httpClient = AnthropicOkHttpClient.builder()
                .apiKey(SecretsUtils.getSecretText(configuration.apiKeyCredentialsId, null))
                .build();
        ModelInfo model = httpClient.models().retrieve(configuration.modelName);
        ret.setProviderType(ModelProviderType.ANTHROPIC);
        ret.setMaxTemperature(1.0d);
        if (model.capabilities().isEmpty()) {
            throw new IOException("No capabilities provided");
        }
        if (model.maxInputTokens().isEmpty() || model.maxTokens().isEmpty()) {
            throw new IOException("No token caps provided");
        }
        if (model.capabilities().get().effort().supported()) {
            ArrayList<ModelThinkingLevel> thinkingLevels = new ArrayList<>();
            if (model.capabilities().get().effort().low().supported()) {
                thinkingLevels.add(ModelThinkingLevel.LOW);
            }
            if (model.capabilities().get().effort().medium().supported()) {
                thinkingLevels.add(ModelThinkingLevel.MEDIUM);
            }
            if (model.capabilities().get().effort().high().supported()) {
                thinkingLevels.add(ModelThinkingLevel.HIGH);
            }
            if (model.capabilities().get().effort().xhigh().isPresent() && model.capabilities().get().effort().xhigh().get().supported()) {
                thinkingLevels.add(ModelThinkingLevel.EXTRA_HIGH);
            }
            if (model.capabilities().get().effort().max().supported()) {
                thinkingLevels.add(ModelThinkingLevel.MAX);
            }
            ret.setSupportedThinkingLevels(thinkingLevels);
        } else {
            ret.setSupportedThinkingLevels(new ArrayList<>());
        }
        ArrayList<ModelInputType> inputs = new ArrayList<>();
        ArrayList<ModelOutputType> outputs = new ArrayList<>();
        outputs.add(ModelOutputType.UNSTRUCTURED_TEXT);
        if (model.capabilities().get().pdfInput().supported()) {
            inputs.add(ModelInputType.PDF);
        }
        if (model.capabilities().get().imageInput().supported()) {
            inputs.add(ModelInputType.IMAGE);
            inputs.add(ModelInputType.VIDEO);
        }
        if (model.capabilities().get().structuredOutputs().supported()) {
            outputs.add(ModelOutputType.STRUCTURED_OUTPUT);
        }
        ret.setInputs(inputs);
        ret.setOutputs(outputs);
        ret.setMaxInputTokens(model.maxInputTokens().get());
        ret.setMaxOutputTokens(model.maxTokens().get() - model.maxInputTokens().get());
        ret.setContextWindow(model.maxInputTokens().get());
        ArrayList<ModelCapability> capabilities = new ArrayList<>();
        if (model.capabilities().get().citations().supported()) {
            capabilities.add(ModelCapability.CITATIONS);
        }
        if (model.capabilities().get().codeExecution().supported()) {
            capabilities.add(ModelCapability.CODE_EXECUTION);
        }
        capabilities.add(ModelCapability.WEB_SEARCH);
        capabilities.add(ModelCapability.TOOLS);
        capabilities.add(ModelCapability.STREAMING);
        ret.setCapabilities(capabilities);
        return ret;
    }
}
