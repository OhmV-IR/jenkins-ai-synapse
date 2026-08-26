package io.ohmvir.plugins.jenkinscr.api.client.impl;

import io.ohmvir.plugins.jenkinscr.api.client.ModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.ModelConversation;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.api.client.ModelResponse;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.configuration.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.OpenAIModelConfiguration;

public class OpenAIModelClient extends ModelClient {
    public OpenAIModelClient(ModelData modelData, OpenAIModelConfiguration configuration, ModelClientConfiguration clientConfiguration) {
        super(modelData, configuration, clientConfiguration);
    }

    @Override
    public ModelResponse generateResponse(ModelRequest request) {
        return null;
    }

    @Override
    public ModelConversation beginConversation() {
        return null;
    }
}
