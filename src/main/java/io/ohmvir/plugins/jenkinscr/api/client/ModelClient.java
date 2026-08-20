package io.ohmvir.plugins.jenkinscr.api.client;

import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.configuration.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;

public abstract class ModelClient {
    public ModelClient(ModelData modelData, ModelConfiguration configuration, ModelClientConfiguration clientConfiguration){

    }

    private String apiBaseUrl;
    private String apiKey;
    private ModelClientConfiguration configuration;

    public abstract ModelResponse generateResponse(ModelRequest request);
    public abstract ModelResponse generateConversationResponse(ModelRequest request, ModelConversation conversation);
    public abstract ModelConversation beginConversation();
}
