package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import java.util.*;
import lombok.Getter;

public class ModelResponse {
    private @Getter final List<ModelOutput> outputs = new ArrayList<>();

    public ModelResponse(List<ModelOutput> outputs) {}
}
