package io.ohmvir.plugins.jenkinscr.configuration.models;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.api.ModelProviderType;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import jenkins.model.Jenkins;

import java.util.Objects;

public abstract class ModelConfiguration implements Describable<ModelConfiguration>, ExtensionPoint {

    public ModelConfiguration(String modelName) throws Descriptor.FormException {
        if(modelName.isBlank()){
            throw new Descriptor.FormException("Model name should not be empty", "modelName");
        }
        this.modelName = modelName;
    }

    public static DescriptorExtensionList<ModelConfiguration, Descriptor<ModelConfiguration>> all(){
        return Jenkins.get().getDescriptorList(ModelConfiguration.class);
    }

    @Override
    public Descriptor<ModelConfiguration> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }

    public String modelName;
    public String modelDisplayName;
    public abstract ModelProviderType getProviderType();

    public String getModelIdDisplayName() {
        return getProviderType().toString() + ":" + modelDisplayName;
    }
    public String getModelId(){
        return getProviderType().toString() + ":" + modelName;
    }

    public static ModelConfiguration getFromId(String id){
        return AgenticCodeReviewSettings.get().getProviders()
                .stream()
                .filter(model -> Objects.equals(model.getProviderType().toString(), id.split(":")[0]))
                .filter(model -> Objects.equals(id.split(":")[1], model.modelName))
                .findFirst().orElse(null);
    }
}
