package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;

public abstract class ModelOutput implements Describable<ModelOutput>, ExtensionPoint, ModelContent {}
