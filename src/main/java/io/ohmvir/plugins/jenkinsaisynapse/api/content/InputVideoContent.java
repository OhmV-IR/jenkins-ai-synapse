package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInputDescriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import lombok.Getter;

import java.util.Set;

public class InputVideoContent extends ModelInput {
    private @Getter final byte[] videoData;

    public InputVideoContent(byte[] videoData){
        this.videoData = videoData;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.VIDEO);
        }
    }
}
