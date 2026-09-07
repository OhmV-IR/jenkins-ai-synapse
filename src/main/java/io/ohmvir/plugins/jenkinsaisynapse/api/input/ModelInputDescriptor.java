package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;

public abstract class ModelInputDescriptor extends Descriptor<ModelInput> {
    public abstract Set<ModelCapability> getRequiredCapabilities();

    public abstract Set<ModelInputType> getRequiredInputTypes();
}
