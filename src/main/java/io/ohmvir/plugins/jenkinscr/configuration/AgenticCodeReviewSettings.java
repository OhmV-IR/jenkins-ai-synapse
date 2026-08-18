package io.ohmvir.plugins.jenkinscr.configuration;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AbstractAgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.Model;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.ArrayList;
import java.util.List;

@Extension
public class AgenticCodeReviewSettings extends GlobalConfiguration {

    private List<Model> providers = new ArrayList<>();
    private List<AbstractAgentConfiguration> agentConfigurations = new ArrayList<>();

    public AgenticCodeReviewSettings(){
        load();
    }

    public static AgenticCodeReviewSettings get() {
        return GlobalConfiguration.all().get(AgenticCodeReviewSettings.class);
    }

    public List<Model> getProviders() {
        return providers;
    }

    @DataBoundSetter
    public void setProviders(List<Model> providers) {
        this.providers = providers != null ? providers : new ArrayList<>();
        save();
    }

    public List<AbstractAgentConfiguration> getAgentConfigurations() {
        return agentConfigurations;
    }

    @DataBoundSetter
    public void setAgentConfigurations(List<AbstractAgentConfiguration> agentConfigurations) {
        this.agentConfigurations = agentConfigurations != null ? agentConfigurations : new ArrayList<>();
        save();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    @Override
    public @NonNull String getDisplayName() {
        return "Agentic Code Review Settings";
    }

}
