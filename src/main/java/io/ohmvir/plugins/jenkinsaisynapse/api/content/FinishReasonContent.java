package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputDescriptor;
import lombok.Getter;

import java.util.Set;

public class FinishReasonContent extends ModelOutput {
    private @Getter final ModelFinishReason reason;

    public FinishReasonContent(ModelFinishReason reason){
        this.reason = reason;
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.PREMATURE_STOP);
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of();
        }
    }
}
