package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A citation of a document the model was given, supporting the text before it.
 */
public final class CitationPart implements ContentPart {
    private final String citedText;
    private final int documentIndex;
    private final @Nullable String documentTitle;
    private final @Nullable String fileId;
    private final int startCharIndex;
    private final int endCharIndex;

    public CitationPart(
            String citedText,
            int documentIndex,
            @Nullable String documentTitle,
            @Nullable String fileId,
            int startCharIndex,
            int endCharIndex) {
        this.citedText = Objects.requireNonNull(citedText, "citedText");
        this.documentIndex = documentIndex;
        this.documentTitle = documentTitle;
        this.fileId = fileId;
        this.startCharIndex = startCharIndex;
        this.endCharIndex = endCharIndex;
    }

    public String getCitedText() {
        return citedText;
    }

    public int getDocumentIndex() {
        return documentIndex;
    }

    public @Nullable String getDocumentTitle() {
        return documentTitle;
    }

    public @Nullable String getFileId() {
        return fileId;
    }

    public int getStartCharIndex() {
        return startCharIndex;
    }

    public int getEndCharIndex() {
        return endCharIndex;
    }
}
