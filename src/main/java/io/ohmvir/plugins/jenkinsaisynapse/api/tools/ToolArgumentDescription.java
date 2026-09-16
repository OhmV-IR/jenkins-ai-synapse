package io.ohmvir.plugins.jenkinsaisynapse.api.tools;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class ToolArgumentDescription implements Describable<ToolArgumentDescription>, ExtensionPoint {
    private @Getter final String name;
    private @Getter final Class<?> type;
    private @Getter final String description;

    @DataBoundConstructor
    public ToolArgumentDescription(String name, Class<?> type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public String getTypeName() {
        return type.getName();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ToolArgumentDescription> {
        @Override
        public @NonNull String getDisplayName() {
            return "Tool Argument Description";
        }
    }
}
