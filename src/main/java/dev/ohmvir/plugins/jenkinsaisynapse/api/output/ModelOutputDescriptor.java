package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import hudson.model.Descriptor;
import java.util.Set;

public abstract class ModelOutputDescriptor extends Descriptor<ModelOutput> {
    public abstract Set<ModelCapability> getRequiredCapabilities();

    public abstract Set<ModelOutputType> getRequiredOutputTypes();
}
