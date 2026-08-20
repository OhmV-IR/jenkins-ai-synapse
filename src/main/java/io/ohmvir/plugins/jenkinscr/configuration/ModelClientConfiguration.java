package io.ohmvir.plugins.jenkinscr.configuration;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class ModelClientConfiguration implements Describable<ModelClientConfiguration>, ExtensionPoint {
    @DataBoundConstructor
    public ModelClientConfiguration(Long timeoutSeconds) throws Descriptor.FormException {
        if(timeoutSeconds == null){
            throw new Descriptor.FormException("Timeout seconds must not be null", "timeoutSeconds");
        }
        if(timeoutSeconds < 0){
            throw new Descriptor.FormException("Timeout seconds must not be negative", "timeoutSeconds");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    public ModelClientConfiguration(){

    }

    private @Getter Long timeoutSeconds = 300L;

    @DataBoundSetter
    public void setTimeoutSeconds(Long timeoutSeconds){
        this.timeoutSeconds = timeoutSeconds;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ModelClientConfiguration> {
        @Override
        public @NonNull String getDisplayName() {
            return "Model Client Configuration";
        }

        public FormValidation doCheckTimeoutSeconds(@QueryParameter Long timeoutSeconds) {
            if(timeoutSeconds == null){
                return FormValidation.error("Timeout seconds is required");
            }
            if(timeoutSeconds < 0){
                return FormValidation.error("Timeout seconds must not be negative");
            }
            return FormValidation.ok();
        }
    }
}
