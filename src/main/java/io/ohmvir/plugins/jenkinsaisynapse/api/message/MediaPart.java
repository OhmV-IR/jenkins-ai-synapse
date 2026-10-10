package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.utils.MimeTypeUtils;
import java.util.Base64;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An image, audio clip, video or file, sent to the model or produced by it.
 */
public final class MediaPart implements ContentPart {
    public enum Kind {
        IMAGE(ModelInputType.IMAGE, ModelOutputType.IMAGE, "image"),
        AUDIO(ModelInputType.AUDIO, ModelOutputType.AUDIO, "audio"),
        VIDEO(ModelInputType.VIDEO, ModelOutputType.VIDEO, "video"),
        FILE(ModelInputType.FILE, ModelOutputType.FILE, null);

        private final ModelInputType inputType;
        private final ModelOutputType outputType;
        private final @Nullable String mimeTopLevelType;

        Kind(ModelInputType inputType, ModelOutputType outputType, @Nullable String mimeTopLevelType) {
            this.inputType = inputType;
            this.outputType = outputType;
            this.mimeTopLevelType = mimeTopLevelType;
        }

        public ModelInputType getInputType() {
            return inputType;
        }

        public ModelOutputType getOutputType() {
            return outputType;
        }
    }

    private final Kind kind;
    private final @Nullable String name;
    private final String mediaType;
    private final byte[] data;

    /**
     * @param kind What the media is.
     * @param name A file name for the media, if it has one.
     * @param mediaType The MIME type identifying the encoding, such as {@code image/png}, {@code audio/ogg; codecs=opus}
     *                  or {@code application/pdf}. Images, audio and video need an {@code image/}, {@code audio/} or
     *                  {@code video/} type.
     * @param data The media's bytes.
     * @throws IllegalArgumentException If the media type is not a valid MIME type for the kind of media.
     */
    public MediaPart(Kind kind, @Nullable String name, String mediaType, byte[] data) {
        this.kind = Objects.requireNonNull(kind, "kind");
        String trimmed = mediaType == null ? null : mediaType.trim();
        if (!MimeTypeUtils.isMimeType(trimmed, kind.mimeTopLevelType)) {
            throw new IllegalArgumentException(
                    kind.mimeTopLevelType == null
                            ? "Invalid MIME type: " + mediaType
                            : "Invalid " + kind.mimeTopLevelType + " MIME type: " + mediaType);
        }
        this.name = name;
        this.mediaType = trimmed;
        this.data = Objects.requireNonNull(data, "data").clone();
    }

    public Kind getKind() {
        return kind;
    }

    public @Nullable String getName() {
        return name;
    }

    public String getMediaType() {
        return mediaType;
    }

    /**
     * @return A copy of the media's bytes.
     */
    public byte[] getData() {
        return data.clone();
    }

    public int getSize() {
        return data.length;
    }

    /**
     * @return The media as a {@code data:} URI, or null if it is empty.
     */
    public @Nullable String getDataUri() {
        if (data.length == 0) {
            return null;
        }
        return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(data);
    }

    @Override
    public String toString() {
        return "MediaPart[" + kind + ", " + name + ", " + mediaType + ", " + data.length + " bytes]";
    }
}
