package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import hudson.Extension;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class InputToolContent extends ModelInput {
    private @Getter final Tool tool;

    @DataBoundConstructor
    public InputToolContent(Tool tool) {
        this.tool = tool;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.TOOLS);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of();
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Input Tool Content";
        }
    }
}
