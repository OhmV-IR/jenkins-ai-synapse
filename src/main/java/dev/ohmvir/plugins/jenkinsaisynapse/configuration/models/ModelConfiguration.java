package dev.ohmvir.plugins.jenkinsaisynapse.configuration.models;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import lombok.Getter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public abstract class ModelConfiguration implements Describable<ModelConfiguration>, ExtensionPoint {

    private @Getter final String modelName;
    private @Getter final String modelDisplayName;

    public ModelConfiguration(String modelName, String modelDisplayName) throws Descriptor.FormException {
        if (modelDisplayName.isBlank()) {
            throw new Descriptor.FormException("Model display name should not be empty", "modelDisplayName");
        }
        this.modelDisplayName = modelDisplayName;
        if (modelName.isBlank()) {
            throw new Descriptor.FormException("Model name should not be empty", "modelName");
        }
        this.modelName = modelName;
    }

    public abstract String getProviderType();

    public String getModelIdDisplayName() {
        return getProviderType() + ":" + modelDisplayName;
    }

    public String getModelId() {
        return getProviderType() + ":" + modelName;
    }

    public abstract static class DescriptorImpl extends Descriptor<ModelConfiguration> {
        @POST
        public FormValidation doCheckModelName(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Model name is required");
            }
            return FormValidation.ok();
        }
    }
}
