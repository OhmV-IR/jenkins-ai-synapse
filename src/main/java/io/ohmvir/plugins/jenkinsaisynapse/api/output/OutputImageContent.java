package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.awt.image.BufferedImage;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class OutputImageContent extends ModelOutput {
    private @Getter final BufferedImage image;

    @DataBoundConstructor
    public OutputImageContent(BufferedImage image) {
        this.image = image;
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.IMAGE);
        }
    }
}
