package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import lombok.Getter;

public class ThinkingLevelContent implements ModelInput {
    private @Getter ModelThinkingLevel thinkingLevel;

    public ThinkingLevelContent(ModelThinkingLevel thinkingLevel) {
        this.thinkingLevel = thinkingLevel;
    }
}
