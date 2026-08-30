package io.ohmvir.plugins.jenkinscr.configuration;

import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.security.Permission;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.configuration.prompts.PromptConfiguration;
import jakarta.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.model.Loadable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Extension
public class BasePromptsManagementLink extends ManagementLink implements Saveable, Loadable {
    private final HashMap<String, PromptConfiguration> prompts = new HashMap<>();

    public BasePromptsManagementLink() throws IOException {
        load();
    }

    public static BasePromptsManagementLink get(){
        return ExtensionList.lookupSingleton(BasePromptsManagementLink.class);
    }

    public @Nullable ModelRequest getRequestFromPromptId(String promptId){
        return prompts.get(promptId).CreateRequest();
    }

    public @Nullable PromptConfiguration getPromptConfiguration(String promptId){
        return prompts.get(promptId);
    }

    public List<PromptConfiguration> getPrompts(){
        return prompts.values().stream().toList();
    }

    public List<Descriptor<PromptConfiguration>> getPromptDescriptors(){
        return Jenkins.get().getDescriptorList(PromptConfiguration.class);
    }

    @Override
    public String getIconFileName() {
        return "symbol-settings-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "Base Prompts";
    }

    @Override
    public String getUrlName() {
        return "prompts";
    }

    @Override
    public String getDescription() {
        return "Manage and configure base prompts to be adapted for workflows";
    }

    @Override
    public @NonNull Category getCategory() {
        return Category.CONFIGURATION;
    }

    @Override
    public @NonNull Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    @RequirePOST
    public void doConfigSubmit(StaplerRequest2 req, StaplerResponse2 res)
        throws IOException, Descriptor.FormException, ServletException {
        Jenkins.get().checkPermission(getRequiredPermission());
        BulkChange change = new BulkChange(this);
        try {
            for(PromptConfiguration config : req.bindJSONToList(PromptConfiguration.class, req.getSubmittedForm().get("prompts"))){
                prompts.put(config.getPromptId(), config);
            }
            save();
            change.commit();
        } finally {
            change.abort();
        }
        res.sendRedirect2(req.getContextPath() + "/" + getUrlName());
    }

    protected XmlFile getConfigFile(){
        return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.get().getRootDir(), getUrlName() + ".xml"));
    }

    @Override
    public void save() throws IOException {
        getConfigFile().write(this);
    }

    @Override
    public void load() throws IOException {
        XmlFile file = getConfigFile();
        if(file.exists()){
            file.unmarshal(this);
        }
    }
}
