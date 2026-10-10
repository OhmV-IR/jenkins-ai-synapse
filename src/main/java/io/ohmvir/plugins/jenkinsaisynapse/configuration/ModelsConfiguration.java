package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.Extension;
import hudson.ExtensionList;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Stores the models available for executing requests. Edited from {@link ModelsManagementLink}, and can be set
 * with configuration as code under {@code unclassified.aiModels}.
 */
@Extension
@Symbol("aiModels")
public class ModelsConfiguration extends GlobalConfiguration {
    private static final Logger LOGGER = Logger.getLogger(ModelsConfiguration.class.getName());
    private static final String LEGACY_FILE = "models.xml";

    private volatile List<ModelConfiguration> models = new ArrayList<>();

    public ModelsConfiguration() {
        load();
        if (!getConfigFile().exists()) {
            LegacyModels legacy = LegacyConfigFile.read(LEGACY_FILE, ModelsManagementLink.class, LegacyModels.class);
            if (legacy != null) {
                if (legacy.modelConfigurations != null) {
                    models = new ArrayList<>(legacy.modelConfigurations);
                }
                save();
                LegacyConfigFile.markMigrated(LEGACY_FILE);
            }
        }
    }

    public static ModelsConfiguration get() {
        return ExtensionList.lookupSingleton(ModelsConfiguration.class);
    }

    public List<ModelConfiguration> getModels() {
        return Collections.unmodifiableList(models);
    }

    @DataBoundSetter
    public void setModels(List<ModelConfiguration> models) {
        this.models = models == null ? new ArrayList<>() : new ArrayList<>(models);
        save();
        for (ModelConfiguration model : this.models) {
            try {
                ModelData.initializeModelDataForConfig(model);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve model data for " + model.getModelId(), e);
            }
        }
    }

    @Override
    public @NonNull String getDisplayName() {
        return "AI Models";
    }

    /** Shape of the file written by {@link ModelsManagementLink} before this class existed. */
    private static final class LegacyModels {
        private List<ModelConfiguration> modelConfigurations;
    }
}
