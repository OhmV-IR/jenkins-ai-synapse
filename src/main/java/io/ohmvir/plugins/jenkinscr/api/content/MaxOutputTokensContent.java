package io.ohmvir.plugins.jenkinscr.api.content;

import io.ohmvir.plugins.jenkinscr.api.input.ModelInput;
import lombok.Getter;

public class MaxOutputTokensContent implements ModelInput {
    private @Getter final long maxOutputTokens;

    public MaxOutputTokensContent(long maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }
}
