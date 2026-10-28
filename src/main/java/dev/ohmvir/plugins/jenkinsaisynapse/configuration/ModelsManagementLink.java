package dev.ohmvir.plugins.jenkinsaisynapse.configuration;

import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Page for editing {@link ModelsConfiguration}.
 */
@Extension
public class ModelsManagementLink extends ManagementLink {
    public static ModelsManagementLink get() {
        return ExtensionList.lookupSingleton(ModelsManagementLink.class);
    }

    public List<ModelConfiguration> getModels() {
        return ModelsConfiguration.get().getModels();
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
        List<ModelConfiguration> models = FormLists.bind(req, ModelConfiguration.class, "modelConfigurations");
        ModelsConfiguration configuration = ModelsConfiguration.get();
        try (BulkChange change = new BulkChange(configuration)) {
            configuration.setModels(models);
            change.commit();
        }
        res.sendRedirect2(".");
    }
}
