package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;

public class ModelResponse {
    private @Getter final List<ModelOutput> outputs;

    public ModelResponse(List<ModelOutput> outputs) {
        this.outputs = List.copyOf(outputs);
    }

    public String getText() {
        return getOutputs(OutputTextContent.class).stream()
                .map(OutputTextContent::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    public <T extends ModelOutput> List<T> getOutputs(Class<T> outputType) {
        return outputs.stream()
                .filter(outputType::isInstance)
                .map(outputType::cast)
                .toList();
    }
}
