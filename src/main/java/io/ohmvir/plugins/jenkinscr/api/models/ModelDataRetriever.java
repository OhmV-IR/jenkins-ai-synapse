package io.ohmvir.plugins.jenkinscr.api.models;

import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;

import java.io.IOException;

public abstract class ModelDataRetriever<T extends ModelConfiguration> {
    private final Class<T> configurationClass;

    public ModelDataRetriever(Class<T> configurationClass) {
        this.configurationClass = configurationClass;
    }

    public abstract ModelData retrieveFromConfiguration(T configuration) throws IOException, InterruptedException;

    public ModelData retrieveFromConfigurationGen(ModelConfiguration config) {
        try {
            if (configurationClass.isInstance(config)) {
                return retrieveFromConfiguration(configurationClass.cast(config));
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
