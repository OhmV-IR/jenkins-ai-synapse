package dev.ohmvir.plugins.jenkinsaisynapse.configuration;

import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import hudson.Extension;
import hudson.ExtensionList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Stores the base prompts. Edited from {@link BasePromptsManagementLink}, and can be set with configuration as code
 * under {@code unclassified.aiPrompts}.
 */
@Extension
@Symbol("aiPrompts")
public class PromptsConfiguration extends GlobalConfiguration {
    private static final String LEGACY_FILE = "prompts.xml";

    private volatile List<PromptConfiguration> prompts = new ArrayList<>();

    public PromptsConfiguration() {
        load();
        if (!getConfigFile().exists()) {
            LegacyPrompts legacy =
                    LegacyConfigFile.read(LEGACY_FILE, BasePromptsManagementLink.class, LegacyPrompts.class);
            if (legacy != null) {
                if (legacy.prompts != null) {
                    prompts = new ArrayList<>(legacy.prompts.values());
                }
                save();
                LegacyConfigFile.markMigrated(LEGACY_FILE);
            }
        }
    }

    public static PromptsConfiguration get() {
        return ExtensionList.lookupSingleton(PromptsConfiguration.class);
    }

    public List<PromptConfiguration> getPrompts() {
        return Collections.unmodifiableList(prompts);
    }

    /**
     * Replaces all prompts. Prompts that are not in the new list are removed.
     *
     * @throws IllegalArgumentException if two prompts share a prompt id
     */
    @DataBoundSetter
    public void setPrompts(List<PromptConfiguration> prompts) {
        List<PromptConfiguration> newPrompts = prompts == null ? new ArrayList<>() : new ArrayList<>(prompts);
        Set<String> ids = new HashSet<>();
        for (PromptConfiguration prompt : newPrompts) {
            if (!ids.add(prompt.getPromptId())) {
                throw new IllegalArgumentException("Prompt id '" + prompt.getPromptId() + "' is used more than once");
            }
        }
        this.prompts = newPrompts;
        save();
    }

    public @Nullable PromptConfiguration getPrompt(String promptId) {
        return prompts.stream()
                .filter(prompt -> Objects.equals(prompt.getPromptId(), promptId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public @NonNull String getDisplayName() {
        return "AI Base Prompts";
    }

    /** Shape of the file written by {@link BasePromptsManagementLink} before this class existed. */
    private static final class LegacyPrompts {
        private HashMap<String, PromptConfiguration> prompts;
    }
}
