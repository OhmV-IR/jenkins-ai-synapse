package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;

public class OutputImageContent extends ModelOutput {
    public static final ModelOutputKind<OutputImageContent> KIND =
            new ModelOutputKind<>(OutputImageContent.class, Set.of(), Set.of(ModelOutputType.IMAGE));

    private @Getter final byte[] imageData;
    private @Getter final String mimeType; // MIME type identifying the encoding, e.g. image/png or image/jpeg

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

    @Override
    public ModelOutputKind<OutputImageContent> getKind() {
        return KIND;
    }
}
