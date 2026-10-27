package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import hudson.ExtensionPoint;
import hudson.model.Describable;

/**
 * Something a model produced. Plain data: each subclass declares a {@link ModelOutputKind} saying what a model needs to
 * produce it, and needs no running Jenkins.
 */
public abstract class ModelOutput implements Describable<ModelOutput>, ExtensionPoint, ModelContent {
    public abstract ModelOutputKind<?> getKind();
}
