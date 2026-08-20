package io.ohmvir.plugins.jenkinscr.api.models;

import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;

public abstract class ModelDataRetriever<T extends ModelConfiguration> {
    private final Class<T> configurationClass;
    public ModelDataRetriever(Class<T> configurationClass){
        this.configurationClass = configurationClass;
    }
    public abstract ModelData retrieveFromConfiguration(T configuration);

    public ModelData retrieveFromConfigurationGen(ModelConfiguration config){
        if(configurationClass.isInstance(config)){
            return retrieveFromConfiguration(configurationClass.cast(config));
        }
        return null;
    }
}
