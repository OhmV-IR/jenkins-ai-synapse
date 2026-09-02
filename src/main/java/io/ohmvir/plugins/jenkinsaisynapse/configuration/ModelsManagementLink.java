package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.security.Permission;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import jakarta.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import jenkins.model.Loadable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
public class ModelsManagementLink extends ManagementLink implements Saveable, Loadable {
    private @Getter List<ModelConfiguration> modelConfigurations = new ArrayList<>();

    public ModelsManagementLink() throws IOException {
        load();
    }

    public static ModelsManagementLink get() {
        return ExtensionList.lookupSingleton(ModelsManagementLink.class);
    }

    public List<Descriptor<ModelConfiguration>> getModelDescriptors() {
        return Jenkins.get().getDescriptorList(ModelConfiguration.class);
    }

    @Override
    public String getIconFileName() {
        return "symbol-settings-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "Models";
    }

    @Override
    public String getUrlName() {
        return "models";
    }

    @Override
    public String getDescription() {
        return "Manage and configure models available for executing requests";
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
            this.modelConfigurations = req.bindJSONToList(
                    ModelConfiguration.class, req.getSubmittedForm().get("modelConfigurations"));
            save();
            change.commit();
        } finally {
            change.abort();
        }
        modelConfigurations.forEach(ModelData::initializeModelDataForConfig);
        res.sendRedirect2(req.getContextPath() + "/" + getUrlName());
    }

    protected XmlFile getConfigFile() {
        return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.get().getRootDir(), getUrlName() + ".xml"));
    }

    @Override
    public void save() throws IOException {
        getConfigFile().write(this);
    }

    @Override
    public void load() throws IOException {
        XmlFile file = getConfigFile();
        if (file.exists()) {
            file.unmarshal(this);
        }
    }
}
