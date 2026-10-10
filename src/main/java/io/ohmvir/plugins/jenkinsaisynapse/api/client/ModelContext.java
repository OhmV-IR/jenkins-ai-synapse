package io.ohmvir.plugins.jenkinsaisynapse.api.client;

import io.ohmvir.plugins.jenkinsaisynapse.api.agent.AgentListener;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.Objects;

/**
 * Everything a {@link ModelClient} needs besides the transcript: the model and its configuration, and the request's
 * system prompt, settings, tools and skills.
 */
public final class ModelContext<C extends ModelConfiguration, CC extends ModelClientConfiguration> {
    private final ModelData model;
    private final C configuration;
    private final CC clientConfiguration;
    private final ModelRequest request;
    private final AgentListener listener;

    public ModelContext(
            ModelData model, C configuration, CC clientConfiguration, ModelRequest request, AgentListener listener) {
        this.model = Objects.requireNonNull(model, "model");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.clientConfiguration = Objects.requireNonNull(clientConfiguration, "clientConfiguration");
        this.request = Objects.requireNonNull(request, "request");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public ModelData getModel() {
        return model;
    }

    public C getConfiguration() {
        return configuration;
    }

    public CC getClientConfiguration() {
        return clientConfiguration;
    }

    /**
     * @return The request being run, for its system prompt, generation settings, tools and skills. Send
     * the transcript passed to {@link ModelClient#step} rather than the request's messages.
     */
    public ModelRequest getRequest() {
        return request;
    }

    /**
     * @return The listener to report streamed text and thinking to, as it is generated.
     */
    public AgentListener getListener() {
        return listener;
    }
}
