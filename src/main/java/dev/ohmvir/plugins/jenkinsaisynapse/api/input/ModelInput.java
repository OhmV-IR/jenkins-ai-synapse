package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import hudson.ExtensionPoint;
import hudson.model.Describable;

public abstract class ModelInput implements Describable<ModelInput>, ExtensionPoint, ModelContent {}
