package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;

public class InputImageContent extends ModelInput {
    private @Getter final byte[] imageData;
    private @Getter final String mimeType; // MIME type identifying the encoding, e.g. image/png or image/jpeg

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

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of();
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of(ModelInputType.IMAGE);
    }
}
