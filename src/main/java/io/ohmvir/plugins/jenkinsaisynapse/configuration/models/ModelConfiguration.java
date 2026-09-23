package io.ohmvir.plugins.jenkinsaisynapse.configuration.models;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import java.util.Objects;
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

    public static ModelConfiguration getFromId(String id) {
        return ModelsManagementLink.get().getModelConfigurations().stream()
                .filter(model -> Objects.equals(model.getProviderType(), id.split(":")[0]))
                .filter(model -> Objects.equals(id.split(":")[1], model.modelName))
                .findFirst()
                .orElse(null);
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
