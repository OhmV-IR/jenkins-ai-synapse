package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class InputVideoContent extends ModelInput {
    private @Getter final byte[] videoData;

    @DataBoundConstructor
    public InputVideoContent(byte[] videoData) {
        this.videoData = videoData;
    }

    public void doVideo(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        if (videoData == null || videoData.length == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        rsp.setContentType("video/mp4");
        rsp.setContentLength(videoData.length);
        rsp.getOutputStream().write(videoData);
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.VIDEO);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Input Video Content";
        }
    }
}
