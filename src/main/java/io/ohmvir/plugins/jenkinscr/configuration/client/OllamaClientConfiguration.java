package io.ohmvir.plugins.jenkinscr.configuration.client;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class OllamaClientConfiguration extends ModelClientConfiguration {
    private @Getter Long keepAliveSeconds = 0L;

    @DataBoundSetter
    public void setKeepAliveTime(Long newKeepAliveSeconds){
        this.keepAliveSeconds = newKeepAliveSeconds;
    }

    @DataBoundConstructor
    public OllamaClientConfiguration(Long timeoutSeconds, Long keepAliveSeconds) throws Descriptor.FormException {
        super(timeoutSeconds);
        this.keepAliveSeconds = keepAliveSeconds;
    }

    @Extension
    public static class DescriptorImpl extends ModelClientConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Ollama Client Configuration";
        }

        public FormValidation doCheckKeepAliveSeconds(@QueryParameter Long keepAliveSeconds) {
            if (keepAliveSeconds == null) {
                return FormValidation.error("Timeout seconds is required");
            }
            if (keepAliveSeconds < 0) {
                return FormValidation.error("Timeout seconds must not be negative");
            }
            return FormValidation.ok();
        }
    }
}
