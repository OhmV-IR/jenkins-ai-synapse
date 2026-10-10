package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.Objects;

/**
 * A configured model together with the provider that handles it and what it can do.
 */
public record RegisteredModel(ModelConfiguration configuration, ModelProvider<?> provider, ModelData data) {
    public RegisteredModel {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(data, "data");
    }

    public String getModelId() {
        return configuration.getModelId();
    }

    public ModelClient createClient() {
        return provider.createModelClient(configuration);
    }
}
