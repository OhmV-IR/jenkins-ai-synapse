package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class OutputFileContent extends ModelOutput {
    public static final ModelOutputKind<OutputFileContent> KIND =
            new ModelOutputKind<>(OutputFileContent.class, Set.of(), Set.of(ModelOutputType.FILE));

    private @Getter final String fileId;
    private @Getter final byte[] fileData;
    private @Getter final String contentType; // MIME content type

    public OutputFileContent(String fileId, byte[] fileData, String contentType) {
        this.fileId = fileId;
        this.fileData = fileData;
        this.contentType = contentType;
    }

    public void doDownload(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        if (fileData == null || fileData.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        rsp.setContentType(contentType);
        rsp.setContentLength(fileData.length);
        rsp.setHeader("Content-Disposition", "attachment; filename=\"" + fileId + "\"");
        rsp.getOutputStream().write(fileData);
    }

    @Override
    public ModelOutputKind<OutputFileContent> getKind() {
        return KIND;
    }
}
