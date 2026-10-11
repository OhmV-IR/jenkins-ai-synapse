package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import hudson.Extension;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class OutputAudioContent extends ModelOutput {
    private @Getter final byte[] audioData;
    private @Getter final String mimeType;

    /**
     * @param audioData the encoded audio bytes
     * @param mimeType the MIME type identifying the audio encoding, e.g. {@code audio/mpeg}, {@code audio/wav}
     *     or {@code audio/ogg; codecs=opus}
     */
    @DataBoundConstructor
    public OutputAudioContent(byte[] audioData, String mimeType) {
        if (mimeType == null || !mimeType.trim().toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw new IllegalArgumentException("mimeType must be an audio MIME type, got: " + mimeType);
        }
        this.audioData = audioData;
        this.mimeType = mimeType.trim();
    }

    public void doAudio(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        if (audioData == null || audioData.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        rsp.setContentType(mimeType);
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
