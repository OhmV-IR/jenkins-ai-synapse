package io.ohmvir.plugins.jenkinscr.api.client.impl;

import io.ohmvir.plugins.jenkinscr.api.client.ModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.ModelConversation;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.api.client.ModelResponse;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.configuration.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.GeminiModelConfiguration;

public class GeminiModelClient extends ModelClient {
    public GeminiModelClient(ModelData modelData, GeminiModelConfiguration configuration, ModelClientConfiguration clientConfiguration) {
        super(modelData, configuration, clientConfiguration);
    }

    @Override
    public ModelResponse generateResponse(ModelRequest request) {
        return null;
    }

    @Override
    public ModelResponse generateConversationResponse(ModelRequest request, ModelConversation conversation) {
        return null;
    }

    @Override
    public ModelConversation beginConversation() {
        return null;
    }
}
