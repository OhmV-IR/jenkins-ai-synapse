package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import java.util.Set;

/**
 * Something sent to a model. Plain data: it declares what a model needs to accept it, and needs no running Jenkins.
 */
public abstract class ModelInput implements Describable<ModelInput>, ExtensionPoint, ModelContent {
    /**
     * @return The capabilities a model must have to accept this input.
     */
    public abstract Set<ModelCapability> getRequiredCapabilities();

    /**
     * @return The input types a model must accept to accept this input.
     */
    public abstract Set<ModelInputType> getRequiredInputTypes();
}
