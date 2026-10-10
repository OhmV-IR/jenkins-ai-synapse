package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;

public class ModelResponse {
    private @Getter final List<ModelOutput> outputs;

    public ModelResponse(List<ModelOutput> outputs) {
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public <T extends ModelOutput> List<T> getOutputs(Class<T> type) {
        return outputs.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /**
     * @return All text the model produced, in order, separated by blank lines.
     */
    public String getText() {
        return getOutputs(OutputTextContent.class).stream()
                .map(OutputTextContent::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
    }
}
