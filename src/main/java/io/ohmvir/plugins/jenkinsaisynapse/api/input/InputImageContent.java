package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.util.Set;

public class InputImageContent extends ModelInput {
    private @Getter final BufferedImage image;

    public InputImageContent(BufferedImage image){
        this.image = image;
    }

    @Extension
    public static final class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.IMAGE);
        }
    }
}
