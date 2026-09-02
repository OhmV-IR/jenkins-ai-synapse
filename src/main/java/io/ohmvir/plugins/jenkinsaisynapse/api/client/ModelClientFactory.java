package io.ohmvir.plugins.jenkinsaisynapse.api.client;

import hudson.ExtensionPoint;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;

public abstract class ModelClientFactory<ConfigurationType, ClientConfigurationType> implements ExtensionPoint {
    public abstract ModelClient<ConfigurationType, ClientConfigurationType> createClient(ModelData modelData, ConfigurationType modelConfiguration, ClientConfigurationType clientConfiguration);
}
