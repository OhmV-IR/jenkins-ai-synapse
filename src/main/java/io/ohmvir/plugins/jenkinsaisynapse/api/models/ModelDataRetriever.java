package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import hudson.ExtensionPoint;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class ModelDataRetriever<T extends ModelConfiguration> implements ExtensionPoint {
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
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName())
                    .severe(String.format(
                            "Exception thrown when trying to get model data for model configuration with id %s: %s",
                            config.getModelId(), ex.getLocalizedMessage()));
            ex.printStackTrace();
            return null;
        }
    }
}
