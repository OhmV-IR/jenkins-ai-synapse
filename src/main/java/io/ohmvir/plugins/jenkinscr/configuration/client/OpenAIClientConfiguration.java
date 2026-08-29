package io.ohmvir.plugins.jenkinscr.configuration.client;

import hudson.Extension;
import hudson.model.Descriptor;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class OpenAIClientConfiguration extends ModelClientConfiguration {
    @DataBoundConstructor
    public OpenAIClientConfiguration(Long timeoutSeconds) throws Descriptor.FormException {
        super(timeoutSeconds);
    }

    @Extension
    public static class DescriptorImpl extends ModelClientConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "OpenAI Client Configuration";
        }
    }
}
