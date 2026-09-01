package io.ohmvir.plugins.jenkinscr.api.content;

import io.ohmvir.plugins.jenkinscr.api.output.ModelOutput;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class CitationContent implements ModelOutput {
    private @Getter final String citedText;
    private @Getter final int documentIndex;
    private @Getter final @Nullable String documentTitle;
    private @Getter final int endCharIndex;
    private @Getter final @Nullable String fileId;
    private @Getter final int startCharIndex;

    public CitationContent(String citedText, int documentIndex, @Nullable String documentTitle, int endCharIndex, @Nullable String fileId, int startCharIndex){
        this.citedText = citedText;
        this.documentIndex = documentIndex;
        this.documentTitle = documentTitle;
        this.endCharIndex = endCharIndex;
        this.fileId = fileId;
        this.startCharIndex = startCharIndex;
    }
}
