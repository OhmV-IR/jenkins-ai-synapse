package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.MimeTypeUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class OutputVideoContent extends ModelOutput {
    public static final ModelOutputKind<OutputVideoContent> KIND =
            new ModelOutputKind<>(OutputVideoContent.class, Set.of(), Set.of(ModelOutputType.VIDEO));

    private @Getter final byte[] videoData;
    /** MIME type identifying the encoding of {@link #videoData}, eg {@code video/mp4} or {@code video/webm}. */
    private @Getter final String mimeType;

    public OutputVideoContent(byte[] videoData, String mimeType) {
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
    public ModelOutputKind<OutputVideoContent> getKind() {
        return KIND;
    }
}
