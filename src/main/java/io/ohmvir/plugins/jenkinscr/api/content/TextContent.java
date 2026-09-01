package io.ohmvir.plugins.jenkinscr.api.content;

import io.ohmvir.plugins.jenkinscr.api.input.ModelInput;
import io.ohmvir.plugins.jenkinscr.api.output.ModelOutput;
import lombok.Getter;

public class TextContent implements ModelInput, ModelOutput {
    private @Getter final String text;
    public TextContent(String text){
        this.text = text;
    }
}
