package io.ohmvir.plugins.jenkinsaisynapse.configuration.client;

import hudson.ExtensionPoint;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public abstract class ModelClientConfiguration extends GlobalConfiguration implements ExtensionPoint {
    private @Getter Long timeoutSeconds;

    public ModelClientConfiguration(Long timeoutSeconds) throws Descriptor.FormException {
        if (timeoutSeconds == null) {
            throw new Descriptor.FormException("Timeout seconds must not be null", "timeoutSeconds");
        }
        if (timeoutSeconds < 0) {
            throw new Descriptor.FormException("Timeout seconds must not be negative", "timeoutSeconds");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    @DataBoundSetter
    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public abstract static class DescriptorImpl extends Descriptor<GlobalConfiguration> {
        public FormValidation doCheckTimeoutSeconds(@QueryParameter Long timeoutSeconds) {
            if (timeoutSeconds == null) {
                return FormValidation.error("Timeout seconds is required");
            }
            if (timeoutSeconds < 0) {
                return FormValidation.error("Timeout seconds must not be negative");
            }
            return FormValidation.ok();
        }
    }
}
