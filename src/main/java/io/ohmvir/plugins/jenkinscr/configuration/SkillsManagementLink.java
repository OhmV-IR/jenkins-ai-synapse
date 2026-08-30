package io.ohmvir.plugins.jenkinscr.configuration;

import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.security.Permission;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import io.ohmvir.plugins.jenkinscr.configuration.skills.SkillConfiguration;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import jenkins.model.Loadable;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Extension
public class SkillsManagementLink extends ManagementLink implements Saveable, Loadable {
    private List<SkillConfiguration> skillConfigurations = new ArrayList<>();

    public SkillsManagementLink() throws IOException {
        load();
    }

    public static SkillsManagementLink get(){
        return ExtensionList.lookupSingleton(SkillsManagementLink.class);
    }

    public List<SkillConfiguration> getSkills(){
        return Collections.unmodifiableList(skillConfigurations);
    }

    public List<Descriptor<SkillConfiguration>> getSkillDescriptors(){
        return Jenkins.get().getDescriptorList(SkillConfiguration.class);
    }

    @Override
    public String getIconFileName() {
        return "symbol-settings-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "Agent Skills";
    }

    @Override
    public String getDescription() {
        return "Manage installed skills, or add new ones.";
    }

    @Override
    public String getUrlName() {
        return "skills";
    }

    @Override
    public @NonNull Category getCategory() {
        return Category.TOOLS;
    }

    @Override
    public @NonNull Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    @RequirePOST
    public void doConfigSubmit(StaplerRequest2 req, StaplerResponse2 res) throws
            IOException, Descriptor.FormException, jakarta.servlet.ServletException {
        Jenkins.get().checkPermission(getRequiredPermission());
        BulkChange change = new BulkChange(this);
        try {
            this.skillConfigurations = req.bindJSONToList(SkillConfiguration.class, req.getSubmittedForm().get("skillConfigurations"));
            save();
            change.commit();
        } finally {
            change.abort();
        }
        skillConfigurations.forEach(SkillData::initializeSkillDataForConfig);
        res.sendRedirect2(req.getContextPath() + "/" + getUrlName());
    }

    protected XmlFile getConfigFile(){
        return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.get().getRootDir(), getUrlName() + ".xml"));
    }

    @Override
    public void load() throws IOException {
        XmlFile file = getConfigFile();
        if(file.exists()){
            file.unmarshal(this);
        }
    }

    @Override
    public void save() throws IOException {
        getConfigFile().write(this);
    }
}
