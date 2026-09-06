package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputDescriptor;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.util.Set;

public class OutputImageContent extends ModelOutput {
    private @Getter final BufferedImage image;
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
