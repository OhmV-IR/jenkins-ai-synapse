package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import lombok.Getter;

public class MaxOutputTokensContent implements ModelInput {
    private @Getter final long maxOutputTokens;

    public MaxOutputTokensContent(long maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }
}
