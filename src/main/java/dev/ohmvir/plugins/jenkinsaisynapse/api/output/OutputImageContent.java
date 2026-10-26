package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import hudson.Extension;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class OutputImageContent extends ModelOutput {
    private @Getter final byte[] imageData;
    private @Getter final String mimeType; // MIME type identifying the encoding, e.g. image/png or image/jpeg

    @DataBoundConstructor
    public OutputImageContent(byte[] imageData, String mimeType) {
        if (mimeType == null || !mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Image MIME type must be an image/* type, got: " + mimeType);
        }
        this.imageData = imageData;
        this.mimeType = mimeType;
    }

    public String getImageDataUri() {
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageData);
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.IMAGE);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Output Image Content";
        }
    }
}
