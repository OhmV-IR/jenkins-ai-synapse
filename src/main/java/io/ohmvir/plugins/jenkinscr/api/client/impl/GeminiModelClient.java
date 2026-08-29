package io.ohmvir.plugins.jenkinscr.api.client.impl;

import io.ohmvir.plugins.jenkinscr.api.client.ModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.ModelConversation;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.api.client.ModelResponse;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.client.GeminiClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.GeminiModelConfiguration;

public class GeminiModelClient extends ModelClient<GeminiModelConfiguration, GeminiClientConfiguration> {
    public GeminiModelClient(ModelData modelData, GeminiModelConfiguration configuration, GeminiClientConfiguration clientConfiguration) {
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
