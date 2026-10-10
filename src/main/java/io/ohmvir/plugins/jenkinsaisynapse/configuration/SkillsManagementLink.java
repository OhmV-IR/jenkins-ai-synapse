package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.skills.SkillConfiguration;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Page for editing {@link SkillsConfiguration}.
 */
@Extension
public class SkillsManagementLink extends ManagementLink {
    public static SkillsManagementLink get() {
        return ExtensionList.lookupSingleton(SkillsManagementLink.class);
    }

    public List<SkillConfiguration> getSkills() {
        return SkillsConfiguration.get().getSkills();
    }

    public List<Descriptor<SkillConfiguration>> getSkillDescriptors() {
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
        List<SkillConfiguration> skills = FormLists.bind(req, SkillConfiguration.class, "skillConfigurations");
        SkillsConfiguration configuration = SkillsConfiguration.get();
        try (BulkChange change = new BulkChange(configuration)) {
            configuration.setSkills(skills);
            change.commit();
        }
        res.sendRedirect2(".");
    }
}
