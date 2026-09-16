package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class FinishReasonContent extends ModelOutput {
    private @Getter final ModelFinishReason reason;

    @DataBoundConstructor
    public FinishReasonContent(ModelFinishReason reason) {
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
