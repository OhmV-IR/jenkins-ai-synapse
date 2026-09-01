package io.ohmvir.plugins.jenkinscr.api.content;

import io.ohmvir.plugins.jenkinscr.api.output.ModelOutput;
import lombok.Getter;

public class ThinkingContent implements ModelOutput {
    private @Getter final String thinking;
    public ThinkingContent(String thinking){
        this.thinking = thinking;
    }
}
