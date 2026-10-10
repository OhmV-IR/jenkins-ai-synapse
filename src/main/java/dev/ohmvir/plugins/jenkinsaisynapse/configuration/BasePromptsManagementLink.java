package dev.ohmvir.plugins.jenkinsaisynapse.configuration;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
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
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Page for editing {@link PromptsConfiguration}.
 */
@Extension
public class BasePromptsManagementLink extends ManagementLink {
    public static BasePromptsManagementLink get() {
        return ExtensionList.lookupSingleton(BasePromptsManagementLink.class);
    }

    public @Nullable ModelRequest getRequestFromPromptId(String promptId) {
        PromptConfiguration prompt = getPromptConfiguration(promptId);
        return prompt == null ? null : prompt.createRequest();
    }

    public @Nullable PromptConfiguration getPromptConfiguration(String promptId) {
        return PromptsConfiguration.get().getPrompt(promptId);
    }

    public List<PromptConfiguration> getPrompts() {
        return PromptsConfiguration.get().getPrompts();
    }

    public List<Descriptor<PromptConfiguration>> getPromptDescriptors() {
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
        List<PromptConfiguration> prompts = FormLists.bind(req, PromptConfiguration.class, "prompts");
        PromptsConfiguration configuration = PromptsConfiguration.get();
        try (BulkChange change = new BulkChange(configuration)) {
            configuration.setPrompts(prompts);
            change.commit();
        } catch (IllegalArgumentException e) {
            throw new Descriptor.FormException(e.getMessage(), "prompts");
        }
        res.sendRedirect2(".");
    }
}
