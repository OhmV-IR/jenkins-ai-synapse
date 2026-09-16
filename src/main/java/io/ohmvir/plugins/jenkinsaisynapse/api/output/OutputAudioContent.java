package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class OutputAudioContent extends ModelOutput {
    private @Getter final byte[] audioData;

    @DataBoundConstructor
    public OutputAudioContent(byte[] audioData) {
        this.audioData = audioData;
    }

    public void doAudio(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        if (audioData == null || audioData.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        rsp.setContentType("audio/mpeg");
        rsp.setContentLength(audioData.length);
        rsp.getOutputStream().write(audioData);
    }

    @Extension
    public static final class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.AUDIO);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Output Audio Content";
        }
    }
}
