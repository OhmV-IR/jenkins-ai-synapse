package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;

/**
 * Something sent to a model. Plain data: it declares what a model needs to accept it, and needs no running Jenkins.
 */
public abstract class ModelInput implements ModelContent {
    /**
     * @return The capabilities a model must have to accept this input.
     */
    public abstract Set<ModelCapability> getRequiredCapabilities();

    /**
     * @return The input types a model must accept to accept this input.
     */
    public abstract Set<ModelInputType> getRequiredInputTypes();
}
