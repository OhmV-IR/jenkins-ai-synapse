package io.ohmvir.plugins.jenkinscr.api.client.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import io.ohmvir.plugins.jenkinscr.api.client.ModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.ModelConversation;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.api.client.ModelResponse;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.client.AnthropicClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.AnthropicModelConfiguration;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

import static com.anthropic.models.messages.ContentBlock.Type.TEXT;
import static com.anthropic.models.messages.ContentBlock.Type.THINKING;

public class AnthropicModelClient extends ModelClient<AnthropicModelConfiguration, AnthropicClientConfiguration> {
    private final AnthropicClient client;
    private final Logger logger;
    public AnthropicModelClient(ModelData modelData, AnthropicModelConfiguration configuration, AnthropicClientConfiguration clientConfiguration) {
        super(modelData, configuration, clientConfiguration);
        client = AnthropicOkHttpClient.builder()
                .apiKey(SecretsUtils.getSecretText(configuration.apiKeyCredentialsId, null))
                .timeout(Duration.ofSeconds(clientConfiguration.getTimeoutSeconds()))
                .build();
        logger = Logger.getLogger(AnthropicModelClient.class.getName());
    }

    private void applyContentBlockToResponse(ContentBlock contentBlock, ModelResponse res){
        if(contentBlock.type() == TEXT){
            res.setResponseText(contentBlock.text().get().text());
        }
        else if (contentBlock.type() == THINKING){
            res.setThinkingText(contentBlock.thinking().get().thinking());
        } else {
            logger.warning("applyContentBlockToResponse got a ContentBlock of type " + contentBlock.type().asString() + " it didn't know what to do with, ignoring it");
        }
    }

    @Override
    public @Nullable ModelResponse generateResponse(ModelRequest request) {
        try {
            Message responseMessage = client.messages().create(MessageCreateParams.builder()
                    .messages(
                            List.of(MessageParam.builder()
                                    .content(request.getPromptText())
                                    .role(MessageParam.Role.USER)
                                    .build()
                            )
                    )
                    .system(request.getAgentConfiguration().systemPrompt)
                    .temperature(request.getAgentConfiguration().temperature)
                    .model(modelConfiguration.modelName)
                    .thinking(ThinkingConfigParam.ofAdaptive(ThinkingConfigAdaptive.builder()
                            .build()))
                    .build());
            ModelResponse ret = new ModelResponse();
            responseMessage.content().forEach(contentBlock -> {
                applyContentBlockToResponse(contentBlock, ret);
            });
            return ret;
        } catch (Exception e){
            logger.severe("Anthropic API Client failed to execute model request due to exception " + e.getMessage());
            return null;
        }
    }

    @Override
    public ModelConversation beginConversation() {
        return new ModelConversation();
    }
}
