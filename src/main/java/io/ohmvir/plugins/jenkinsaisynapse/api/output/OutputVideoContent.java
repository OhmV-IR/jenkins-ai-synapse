package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class OutputVideoContent extends ModelOutput {
    public static final String DEFAULT_MIME_TYPE = "video/mp4";

    private @Getter final byte[] videoData;
    private final String mimeType;

    @DataBoundConstructor
    public OutputVideoContent(byte[] videoData, String mimeType) {
        this.videoData = videoData;
        this.mimeType = mimeType;
    }

    /**
     * MIME type identifying the encoding of {@link #getVideoData()}, e.g. {@code video/mp4} or {@code video/webm}.
     * Falls back to {@value #DEFAULT_MIME_TYPE} when unset or not a video type.
     */
    public String getMimeType() {
        if (mimeType == null || !mimeType.trim().toLowerCase(Locale.ROOT).startsWith("video/")) {
            return DEFAULT_MIME_TYPE;
        }
        return mimeType.trim();
    }

    public void doVideo(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        if (videoData == null || videoData.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        rsp.setContentType(getMimeType());
        rsp.setHeader("X-Content-Type-Options", "nosniff");
        rsp.setContentLength(videoData.length);
        rsp.getOutputStream().write(videoData);
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.VIDEO);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Output Video Content";
        }
    }
}
