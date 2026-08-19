package io.ohmvir.plugins.jenkinscr.configuration;

import com.google.gson.JsonParser;
import hudson.Extension;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AbstractAgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Extension
public class AgenticCodeReviewSettings extends GlobalConfiguration {

    private List<ModelConfiguration> providers = new ArrayList<>();
    private List<AbstractAgentConfiguration> agentConfigurations = new ArrayList<>();

    public AgenticCodeReviewSettings(){
        load();
    }

    public static AgenticCodeReviewSettings get() {
        return GlobalConfiguration.all().get(AgenticCodeReviewSettings.class);
    }

    public List<ModelConfiguration> getProviders() {
        return providers;
    }

    @DataBoundSetter
    public void setProviders(List<ModelConfiguration> providers) {
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
        if(json.optBoolean("appendDefaults", false)){
            try {
                this.agentConfigurations.addAll(getDefaultAgents());
            } catch (IOException ignored) {

            }
        }
        save();
        return true;
    }

    protected List<AgentConfiguration> getDefaultAgents() throws IOException, FormException {
        ArrayList<AgentConfiguration> defaults = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] agents = resolver.getResources("classpath*:defaults/agents/*.json");
        for(Resource r : agents){
            if(!r.isReadable()){
                continue;
            }
            try (InputStream is = r.getInputStream()){
                String content = new String(is.readAllBytes());
                defaults.add(AgentConfiguration.fromDefaultJson(JsonParser.parseString(content).getAsJsonObject()));
            }
        }
        return defaults;
    }

    @Override
    public @NonNull String getDisplayName() {
        return "Agentic Code Review Settings";
    }

}
