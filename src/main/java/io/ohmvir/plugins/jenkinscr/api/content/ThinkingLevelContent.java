package io.ohmvir.plugins.jenkinscr.api.content;

import io.ohmvir.plugins.jenkinscr.api.input.ModelInput;
import io.ohmvir.plugins.jenkinscr.api.models.ModelThinkingLevel;
import lombok.Getter;

public class ThinkingLevelContent implements ModelInput {
    private @Getter ModelThinkingLevel thinkingLevel;

    public ThinkingLevelContent(ModelThinkingLevel thinkingLevel) {
        this.thinkingLevel = thinkingLevel;
    }
}
