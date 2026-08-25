package io.ohmvir.plugins.jenkinscr.configuration.agents;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import jenkins.model.Jenkins;

public abstract class AbstractAgentConfiguration
        implements Describable<AbstractAgentConfiguration>, ExtensionPoint {

    public String systemPrompt;
    public double temperature;
    public long maxOutputTokensPerPrompt;
    public String modelId;
    public ModelThinkingLevel thinkingLevel;

    public AbstractAgentConfiguration(String systemPrompt, double temperature, long maxOutputTokensPerPrompt, String modelId, ModelThinkingLevel thinkingLevel) throws Descriptor.FormException {
        this.systemPrompt = systemPrompt;
        if (temperature < 0 || temperature > 1) {
            throw new Descriptor.FormException("temperature must be between 0 and 1 inclusive", "temperature");
        }
        this.temperature = temperature;
        if (maxOutputTokensPerPrompt < 0) {
            throw new Descriptor.FormException("maxOutputTokensPerPrompt cannot be negative", "maxOutputTokensPerPrompt");
        }
        this.maxOutputTokensPerPrompt = maxOutputTokensPerPrompt;
        if (modelId == null || ModelConfiguration.getFromId(modelId) == null) {
            throw new Descriptor.FormException("modelName cannot be null and must refer to an already saved model", "modelName");
        }
        this.modelId = modelId;
        if (thinkingLevel == null) {
            throw new Descriptor.FormException("thinkingLevel cannot be null", "thinkingLevel");
        }
        this.thinkingLevel = thinkingLevel;
    }
}