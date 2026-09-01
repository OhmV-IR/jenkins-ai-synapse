package io.ohmvir.plugins.jenkinscr.api.content;

import io.ohmvir.plugins.jenkinscr.api.input.ModelInput;
import lombok.Getter;

public class SystemPromptContent implements ModelInput {
    private @Getter final String systemPrompt;

    public SystemPromptContent(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
