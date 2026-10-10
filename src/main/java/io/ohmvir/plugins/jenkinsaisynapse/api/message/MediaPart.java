package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An image, audio clip, video or file, sent to the model or produced by it.
 */
public final class MediaPart implements ContentPart {
    public enum Kind {
        IMAGE(ModelInputType.IMAGE, ModelOutputType.IMAGE),
        AUDIO(ModelInputType.AUDIO, ModelOutputType.AUDIO),
        VIDEO(ModelInputType.VIDEO, ModelOutputType.VIDEO),
        FILE(ModelInputType.FILE, ModelOutputType.FILE);

        private final ModelInputType inputType;
        private final ModelOutputType outputType;

        Kind(ModelInputType inputType, ModelOutputType outputType) {
            this.inputType = inputType;
            this.outputType = outputType;
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
     * @param mediaType The MIME type, such as {@code image/png} or {@code application/pdf}.
     * @param data The media's bytes.
     */
    public MediaPart(Kind kind, @Nullable String name, String mediaType, byte[] data) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.name = name;
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType");
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

    @Override
    public String toString() {
        return "MediaPart[" + kind + ", " + name + ", " + mediaType + ", " + data.length + " bytes]";
    }
}
