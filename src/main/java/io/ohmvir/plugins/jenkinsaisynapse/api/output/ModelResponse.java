package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import java.io.IOException;
import java.util.*;
import lombok.Getter;

public class ModelResponse {
    private @Getter final List<ModelOutput> outputs = new ArrayList<>();

    public ModelResponse(List<ModelOutput> outputs) {}
}
