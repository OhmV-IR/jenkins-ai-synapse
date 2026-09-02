package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import lombok.Getter;

public class TextContent implements ModelInput, ModelOutput {
    private @Getter final String text;
    public TextContent(String text){
        this.text = text;
    }
}
