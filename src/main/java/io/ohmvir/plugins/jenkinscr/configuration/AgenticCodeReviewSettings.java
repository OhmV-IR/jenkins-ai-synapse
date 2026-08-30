package io.ohmvir.plugins.jenkinscr.configuration;

import hudson.Extension;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import io.ohmvir.plugins.jenkinscr.configuration.client.*;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.skills.SkillConfiguration;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;

@Extension
public class AgenticCodeReviewSettings extends GlobalConfiguration {

    private @Getter List<ModelConfiguration> models = new ArrayList<>();
    private @Getter List<SkillConfiguration> skills = new ArrayList<>();
    private @Getter AnthropicClientConfiguration anthropicClientConfiguration;
    private @Getter GeminiClientConfiguration geminiClientConfiguration;
    private @Getter OllamaClientConfiguration ollamaClientConfiguration;
    private @Getter OpenAIClientConfiguration openAIClientConfiguration;

    public AgenticCodeReviewSettings() {
        load();
    }

    public static AgenticCodeReviewSettings get() {
        return GlobalConfiguration.all().get(AgenticCodeReviewSettings.class);
    }

    @DataBoundSetter
    public void setModels(List<ModelConfiguration> models) {
        this.models = models != null ? models : new ArrayList<>();
        this.models.forEach(ModelData::initializeModelDataForConfig);
        save();
    }

    @DataBoundSetter
    public void setSkills(List<SkillConfiguration> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
        this.skills.forEach(SkillData::initializeSkillDataForConfig);
        save();
    }

    @DataBoundSetter
    public void setAnthropicClientConfiguration(AnthropicClientConfiguration anthropicClientConfiguration) {
        this.anthropicClientConfiguration = anthropicClientConfiguration;
    }

    @DataBoundSetter
    public void setGeminiClientConfiguration(GeminiClientConfiguration geminiClientConfiguration) {
        this.geminiClientConfiguration = geminiClientConfiguration;
    }

    @DataBoundSetter
    public void setOllamaClientConfiguration(OllamaClientConfiguration ollamaClientConfiguration) {
        this.ollamaClientConfiguration = ollamaClientConfiguration;
    }

    @DataBoundSetter
    public void setOpenAIClientConfiguration(OpenAIClientConfiguration openAIClientConfiguration) {
        this.openAIClientConfiguration = openAIClientConfiguration;
    }

    @Override
    public @NonNull String getDisplayName() {
        return "Agentic Code Review Settings";
    }

}
