package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputDescriptor;
import lombok.Getter;

import java.util.Set;

public class OutputFileContent extends ModelOutput {
    private @Getter final String fileId;
    private @Getter final byte[] fileData;

    public OutputFileContent(String fileId, byte[] fileData) {
        this.fileId = fileId;
        this.fileData = fileData;
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {

        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.FILE);
        }
    }
}
