package io.ohmvir.plugins.jenkinscr.api.client;

import hudson.ExtensionPoint;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;

public abstract class ModelClientFactory<ConfigurationType, ClientConfigurationType> implements ExtensionPoint {
    public abstract ModelClient<ConfigurationType, ClientConfigurationType> CreateClient(ModelData modelData, ConfigurationType modelConfiguration, ClientConfigurationType clientConfiguration);
}
