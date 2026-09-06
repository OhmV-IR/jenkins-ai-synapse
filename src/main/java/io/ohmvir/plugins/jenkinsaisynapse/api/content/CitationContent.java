package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputDescriptor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class CitationContent extends ModelOutput {
    private @Getter final String citedText;
    private @Getter final int documentIndex;
    private @Getter final @Nullable String documentTitle;
    private @Getter final int endCharIndex;
    private @Getter final @Nullable String fileId;
    private @Getter final int startCharIndex;

    public CitationContent(
            String citedText,
            int documentIndex,
            @Nullable String documentTitle,
            int endCharIndex,
            @Nullable String fileId,
            int startCharIndex) {
        this.citedText = citedText;
        this.documentIndex = documentIndex;
        this.documentTitle = documentTitle;
        this.endCharIndex = endCharIndex;
        this.fileId = fileId;
        this.startCharIndex = startCharIndex;
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.CITATIONS);
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of();
        }
    }
}
