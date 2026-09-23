package io.ohmvir.plugins.jenkinsaisynapse.api.client;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.List;

public abstract class ModelClient<
                ConfigurationType extends ModelConfiguration, ClientConfigurationType extends ModelClientConfiguration>
        implements Describable<ModelClient<?, ?>>, ExtensionPoint {
    public ModelClient() {}

    @SuppressWarnings("unchecked")
    public List<ModelOutput> takeStep(
            ModelData modelData,
            ModelConfiguration configuration,
            ModelClientConfiguration clientConfiguration,
            ModelRequest request,
            List<ModelInput> turnInputs) {
        return takeStepImpl(
                modelData,
                (ConfigurationType) configuration,
                (ClientConfigurationType) clientConfiguration,
                request,
                turnInputs);
    }

    /**
     * Generate a response for the given request. This function can assume that the request is able to be handled by the model.
     * @param request The associated model request
     * @param turnInputs The inputs to attach for the model to respond. This will include all context and relevant conversation history, as well as things like tool responses
     * @return A list of all the model outputs that were produced on this exchange (ie API roundtrip)
     */
    protected abstract List<ModelOutput> takeStepImpl(
            ModelData modelData,
            ConfigurationType configuration,
            ClientConfigurationType clientConfiguration,
            ModelRequest request,
            List<ModelInput> turnInputs);
}
