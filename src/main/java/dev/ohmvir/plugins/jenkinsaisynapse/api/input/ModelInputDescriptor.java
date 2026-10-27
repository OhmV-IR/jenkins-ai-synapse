package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import hudson.model.Descriptor;
import java.util.Set;

public abstract class ModelInputDescriptor extends Descriptor<ModelInput> {
    public abstract Set<ModelCapability> getRequiredCapabilities();

    public abstract Set<ModelInputType> getRequiredInputTypes();
}
