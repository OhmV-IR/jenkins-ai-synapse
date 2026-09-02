package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import java.util.List;
import jenkins.model.Jenkins;
import org.jspecify.annotations.NonNull;

@Extension
public class ToolsManagementLink extends ManagementLink {
    @Override
    public String getIconFileName() {
        return "symbol-settings-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "Agent Tools";
    }

    @Override
    public String getUrlName() {
        return "agent-tools";
    }

    @Override
    public String getDescription() {
        return "View installed tools, their descriptions and arguments";
    }

    @Override
    public @NonNull Category getCategory() {
        return Category.CONFIGURATION;
    }

    @Override
    public @NonNull Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    public List<Tool> getTools() {
        return ToolRegistry.getTools().values().stream().toList();
    }
}
