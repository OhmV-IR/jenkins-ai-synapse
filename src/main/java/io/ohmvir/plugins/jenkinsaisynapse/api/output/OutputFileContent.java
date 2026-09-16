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

public class OutputFileContent extends ModelOutput {
    private @Getter final String fileId;
    private @Getter final byte[] fileData;
    private @Getter final String contentType; // MIME content type

    @DataBoundConstructor
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

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {

        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.FILE);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Output File Content";
        }
    }
}
