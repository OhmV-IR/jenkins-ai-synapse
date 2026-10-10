package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;

/**
 * Something a model produced. Plain data: each subclass declares a {@link ModelOutputKind} saying what a model needs to
 * produce it, and needs no running Jenkins.
 */
public abstract class ModelOutput implements ModelContent {
    public abstract ModelOutputKind<?> getKind();
}
