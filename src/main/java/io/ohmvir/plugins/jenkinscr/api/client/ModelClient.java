package io.ohmvir.plugins.jenkinscr.api.client;

import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import lombok.Getter;

import javax.annotation.Nullable;

public abstract class ModelClient<ConfigurationType, ClientConfigurationType> {
    protected @Getter final ClientConfigurationType clientConfiguration;
    protected @Getter final ConfigurationType modelConfiguration;
    protected @Getter final ModelData modelData;

    public ModelClient(ModelData modelData, ConfigurationType configuration, ClientConfigurationType clientConfiguration) {
        this.modelConfiguration = configuration;
        this.clientConfiguration = clientConfiguration;
        this.modelData = modelData;
    }

    public abstract @Nullable ModelResponse generateResponse(ModelRequest request);

    public @Nullable ModelResponse generateConversationResponse(ModelRequest request, ModelConversation conversation){
        request.setConversationHistory(conversation);
        return generateResponse(request);
    }

    public abstract ModelConversation beginConversation();
}
