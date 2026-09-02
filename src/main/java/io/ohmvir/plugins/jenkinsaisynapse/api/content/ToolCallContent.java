package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import java.util.Map;
import lombok.Getter;

public class ToolCallContent implements ModelOutput {
    private @Getter final String id;
    private @Getter final Map<String, String> toolArguments;
    private @Getter final String name;

    public ToolCallContent(String id, Map<String, String> toolArguments, String name) {
        this.id = id;
        this.toolArguments = toolArguments;
        this.name = name;
    }
}
