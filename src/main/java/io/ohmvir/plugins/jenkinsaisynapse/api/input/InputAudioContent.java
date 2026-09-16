package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class InputAudioContent extends ModelInput {
    private @Getter final byte[] audioData;

    @DataBoundConstructor
    public InputAudioContent(byte[] audioData) {
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
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.AUDIO);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Input Audio Content";
        }
    }
}
