package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelConversation;
import lombok.Getter;

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
