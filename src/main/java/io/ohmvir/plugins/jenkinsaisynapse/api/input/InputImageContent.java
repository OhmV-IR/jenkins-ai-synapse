package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class InputImageContent extends ModelInput {
    private @Getter final byte[] imageData;
    private @Getter final String mimeType; // MIME type identifying the encoding, e.g. image/png or image/jpeg

    @DataBoundConstructor
    public InputImageContent(byte[] imageData, String mimeType) {
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
    public static final class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.IMAGE);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Input Image Content";
        }
    }
}
