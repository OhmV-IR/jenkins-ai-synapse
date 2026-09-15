package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;

public abstract class ModelInput implements Describable<ModelInput>, ExtensionPoint, ModelContent {}
