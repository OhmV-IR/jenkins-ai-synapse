package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import lombok.Getter;

public class SystemPromptContent implements ModelInput {
    private @Getter final String systemPrompt;

    public SystemPromptContent(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
