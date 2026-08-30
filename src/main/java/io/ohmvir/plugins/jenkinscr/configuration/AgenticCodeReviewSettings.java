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
    private @Getter AnthropicClientConfiguration anthropicClientConfiguration = new AnthropicClientConfiguration(0L);
    private @Getter GeminiClientConfiguration geminiClientConfiguration = new GeminiClientConfiguration(0L);
    private @Getter OllamaClientConfiguration ollamaClientConfiguration = new OllamaClientConfiguration(0L, 0L);
    private @Getter OpenAIClientConfiguration openAIClientConfiguration = new OpenAIClientConfiguration(0L);

    public AgenticCodeReviewSettings() throws FormException {
        load();
    }

    public static AgenticCodeReviewSettings get() {
        return GlobalConfiguration.all().get(AgenticCodeReviewSettings.class);
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
