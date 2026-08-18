package io.ohmvir.plugins.jenkinscr.configuration.models;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public abstract class Model implements Describable<Model>, ExtensionPoint {

    public Model(String modelName) throws Descriptor.FormException {
        if(modelName.isBlank()){
            throw new Descriptor.FormException("Model name should not be empty", "modelName");
        }
        this.modelName = modelName;
    }

    public static DescriptorExtensionList<Model, Descriptor<Model>> all(){
        return Jenkins.get().getDescriptorList(Model.class);
    }

    @Override
    public Descriptor<Model> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }

    public String modelName;
}
