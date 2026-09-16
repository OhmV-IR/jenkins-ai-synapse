package io.ohmvir.plugins.jenkinsaisynapse.api.client;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import java.util.List;
import lombok.Getter;

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
     * @param request The associated model request
     * @param turnInputs The inputs to attach for the model to respond. This will include all context and relevant conversation history, as well as things like tool responses
     * @return A list of all the model outputs that were produced on this exchange (ie API roundtrip)
     */
    public abstract List<ModelOutput> takeStep(ModelRequest request, List<ModelInput> turnInputs);
}
