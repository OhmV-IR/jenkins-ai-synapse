package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.utils.MimeTypeUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class InputVideoContent extends ModelInput {
    private @Getter final byte[] videoData;
    /** MIME type identifying the encoding of {@link #videoData}, eg {@code video/mp4} or {@code video/webm}. */
    private @Getter final String mimeType;

    public InputVideoContent(byte[] videoData, String mimeType) {
        this.videoData = videoData;
        this.mimeType = MimeTypeUtils.requireVideoMimeType(mimeType, "mimeType");
    }

    public void doVideo(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        if (videoData == null || videoData.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        rsp.setContentType(mimeType);
        rsp.setHeader("X-Content-Type-Options", "nosniff");
        rsp.setContentLength(videoData.length);
        rsp.getOutputStream().write(videoData);
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of();
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of(ModelInputType.VIDEO);
    }
}
