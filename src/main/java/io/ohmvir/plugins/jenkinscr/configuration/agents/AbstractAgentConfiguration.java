package io.ohmvir.plugins.jenkinscr.configuration.agents;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.configuration.models.Model;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public abstract class AbstractAgentConfiguration implements Describable<AbstractAgentConfiguration>, ExtensionPoint {

    public AbstractAgentConfiguration(String systemPrompt, double temperature, long maxOutputTokensPerPrompt, String modelName) throws Descriptor.FormException {
        this.systemPrompt = systemPrompt;
        if(temperature < 0 || temperature > 1){
            throw new Descriptor.FormException("temperature must be between 0 and 1 inclusive", "temperature");
        }
        this.temperature = temperature;
        if(maxOutputTokensPerPrompt < 0){
            throw new Descriptor.FormException("maxOutputTokensPerPrompt cannot be negative", "maxOutputTokensPerPrompt");
        }
        this.maxOutputTokensPerPrompt = maxOutputTokensPerPrompt;
        if(modelName == null){
            throw new Descriptor.FormException("modelName cannot be null", "modelName");
        }
        this.modelName = modelName;
    }

    public String systemPrompt;
    public double temperature;
    public long maxOutputTokensPerPrompt;
    public String modelName;

    @Override
    public Descriptor<AbstractAgentConfiguration> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }
}
