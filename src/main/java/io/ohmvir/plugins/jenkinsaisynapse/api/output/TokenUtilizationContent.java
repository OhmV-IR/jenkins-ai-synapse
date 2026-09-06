package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import lombok.Getter;

import java.util.Set;

public class TokenUtilizationContent extends ModelOutput {
    private @Getter final long inputTokensUsed;
    private @Getter final long outputTokensUsed;
    private @Getter final long cachedTokensUsed;

    public TokenUtilizationContent(long inputTokensUsed, long outputTokensUsed, long cachedTokensUsed) {
        this.inputTokensUsed = inputTokensUsed;
        this.outputTokensUsed = outputTokensUsed;
        this.cachedTokensUsed = cachedTokensUsed;
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.TOKEN_USAGE_METRICS);
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of();
        }
    }
}
