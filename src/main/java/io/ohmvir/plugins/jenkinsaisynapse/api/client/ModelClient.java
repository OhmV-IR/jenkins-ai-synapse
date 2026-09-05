package io.ohmvir.plugins.jenkinsaisynapse.api.client;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public abstract class ModelClient<ConfigurationType, ClientConfigurationType> {
    protected @Getter final ClientConfigurationType clientConfiguration;
    protected @Getter final ConfigurationType modelConfiguration;
    protected @Getter final ModelData modelData;

    public ModelClient(
            ModelData modelData, ConfigurationType configuration, ClientConfigurationType clientConfiguration) {
        this.modelConfiguration = configuration;
        this.clientConfiguration = clientConfiguration;
        this.modelData = modelData;
    }

    /**
     * Generate a response for the given request. This function can assume that the request is able to be handled by the model.
     * @param request The model request to execute
     * @return A valid ModelResponse if the request was executed successfully and null otherwise.
     */
    public abstract @Nullable ModelResponse generateResponse(ModelRequest request);

    public @Nullable ModelResponse generateConversationResponse(ModelRequest request, ModelConversation conversation) {
        request.setConversationHistory(conversation);
        return generateResponse(request);
    }

    public abstract ModelConversation beginConversation();
}
