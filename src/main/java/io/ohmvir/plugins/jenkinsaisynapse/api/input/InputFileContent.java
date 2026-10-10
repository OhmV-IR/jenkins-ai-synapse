package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class InputFileContent extends ModelInput {
    private @Getter final String fileId;
    private @Getter final byte[] fileData;
    private @Getter final String contentType;

    public InputFileContent(String fileId, byte[] fileData, String contentType) {
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
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of();
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of(ModelInputType.FILE);
    }
}
