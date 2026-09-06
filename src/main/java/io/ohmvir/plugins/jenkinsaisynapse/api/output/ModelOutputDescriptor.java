package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;

import java.util.Set;

public abstract class ModelOutputDescriptor extends Descriptor<ModelOutput> {
    public abstract Set<ModelCapability> getRequiredCapabilities();
    public abstract Set<ModelOutputType> getRequiredOutputTypes();
}
