package io.ohmvir.plugins.jenkinscr.configuration;

import hudson.Extension;
import jenkins.model.GlobalConfigurationCategory;
import org.jenkinsci.Symbol;

@Extension
@Symbol("customCategory")
public class AgentManagementCategory extends GlobalConfigurationCategory {
    @Override
    public String getShortDescription() {
        return "Manage models used for agents as well as add or disable tools and skills.";
    }

    @Override
    public String getDisplayName() {
        return "Agent Management";
    }
}
