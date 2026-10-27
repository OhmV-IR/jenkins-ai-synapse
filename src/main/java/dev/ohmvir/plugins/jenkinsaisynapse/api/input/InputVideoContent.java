package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.FileDownloadUtils;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.MimeTypeUtils;
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
        FileDownloadUtils.serveInlineMedia(rsp, mimeType, videoData);
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
