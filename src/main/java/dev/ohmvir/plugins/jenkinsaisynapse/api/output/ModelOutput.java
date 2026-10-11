package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import hudson.ExtensionPoint;
import hudson.model.Describable;

public abstract class ModelOutput implements Describable<ModelOutput>, ExtensionPoint, ModelContent {}
