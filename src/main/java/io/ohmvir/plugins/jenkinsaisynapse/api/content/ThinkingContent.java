package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import lombok.Getter;

public class ThinkingContent implements ModelOutput {
    private @Getter final String thinking;
    public ThinkingContent(String thinking){
        this.thinking = thinking;
    }
}
