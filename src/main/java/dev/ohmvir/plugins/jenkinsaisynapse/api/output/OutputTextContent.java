package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Set;
import lombok.Getter;

public class OutputTextContent extends ModelOutput {
    public static final ModelOutputKind<OutputTextContent> KIND =
            new ModelOutputKind<>(OutputTextContent.class, Set.of(), Set.of(ModelOutputType.UNSTRUCTURED_TEXT));

    private @Getter final String text;

    public OutputTextContent(String text) {
        this.text = text;
    }

    @Override
    public ModelOutputKind<OutputTextContent> getKind() {
        return KIND;
    }
}
