package io.ohmvir.plugins.jenkinscr.configuration;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.jspecify.annotations.NonNull;

public class ModelClientConfiguration implements Describable<ModelClientConfiguration>, ExtensionPoint {
    public long timeoutSeconds = 300;

    @Extension
    public static class DescriptorImpl extends Descriptor<ModelClientConfiguration> {
        @Override
        public @NonNull String getDisplayName() {
            return "Model Client Configuration";
        }
    }
}
