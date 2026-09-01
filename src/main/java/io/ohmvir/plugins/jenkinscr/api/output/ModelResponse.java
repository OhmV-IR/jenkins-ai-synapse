package io.ohmvir.plugins.jenkinscr.api.output;

import io.ohmvir.plugins.jenkinscr.api.input.ModelConversation;
import io.ohmvir.plugins.jenkinscr.api.models.ModelOutputType;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.*;

public class ModelResponse {
    private @Getter final List<ModelOutput> outputs = new ArrayList<>();

    public ModelResponse(){

    }

    public void AddOutput(ModelOutput output) {
        outputs.add(output);
    }

    public void PushToConversation(ModelConversation conversation) throws IOException {
        conversation.AddResponse(this);
    }
}
