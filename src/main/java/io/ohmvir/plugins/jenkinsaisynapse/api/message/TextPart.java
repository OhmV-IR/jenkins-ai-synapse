package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import java.util.Objects;

public final class TextPart implements ContentPart {
    private final String text;

    public TextPart(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "TextPart[" + text + "]";
    }
}
