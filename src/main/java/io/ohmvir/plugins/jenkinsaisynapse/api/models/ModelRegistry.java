package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * The configured models, in the user's order of preference (the order on the Models page). Instances are plain Java
 * and can be used in unit tests; {@link #get()} is the one Jenkins keeps up to date with the Models page.
 */
public class ModelRegistry {
    private static final Logger LOGGER = Logger.getLogger(ModelRegistry.class.getName());
    private static final ModelRegistry GLOBAL = new ModelRegistry();

    private volatile List<RegisteredModel> models = List.of();

    public static ModelRegistry get() {
        return GLOBAL;
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initialize() {
        LOGGER.info("Prefetching model data");
        GLOBAL.reload(ModelsManagementLink.get().getModelConfigurations(), ModelProvider.all());
    }

    /**
     * Replaces the registered models with the given configurations, keeping their order. A configuration that no
     * provider handles, that can't be described, or whose model id is already taken is skipped and logged.
     */
    public void reload(
            List<? extends ModelConfiguration> configurations, Collection<? extends ModelProvider<?>> providers) {
        List<RegisteredModel> loaded = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (ModelConfiguration configuration : configurations) {
            String modelId = configuration.getModelId();
            if (!seenIds.add(modelId)) {
                LOGGER.warning("Skipping duplicate model configuration " + modelId);
                continue;
            }
            ModelProvider<?> provider = ModelProvider.forConfiguration(configuration, providers);
            if (provider == null) {
                LOGGER.severe(
                        "No model provider handles " + configuration.getClass().getName() + " for " + modelId);
                continue;
            }
            try {
                ModelData data = provider.describeModel(configuration);
                if (data == null) {
                    LOGGER.severe(
                            "Model provider " + provider.getClass().getName() + " returned no data for " + modelId);
                    continue;
                }
                loaded.add(new RegisteredModel(configuration, provider, data));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interrupted while getting model data for " + modelId, e);
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to get model data for " + modelId, e);
            }
        }
        models = List.copyOf(loaded);
    }

    /**
     * Replaces the registered models. The list order is the order of preference.
     */
    public void setModels(List<RegisteredModel> models) {
        this.models = List.copyOf(models);
    }

    /**
     * @return The registered models in order of preference.
     */
    public List<RegisteredModel> getModels() {
        return models;
    }

    public Optional<RegisteredModel> getModel(String modelId) {
        return models.stream()
                .filter(model -> Objects.equals(model.getModelId(), modelId))
                .findFirst();
    }

    public @Nullable ModelData getModelData(String modelId) {
        return getModel(modelId).map(RegisteredModel::data).orElse(null);
    }

    /**
     * Picks the model to run a request on. If the request names a model with {@link ModelRequest#setModelId}, that
     * model is used, and only if it can handle the request. Otherwise the first model in order of preference that can
     * handle the request is used.
     *
     * @return null if no suitable model is registered.
     */
    public @Nullable RegisteredModel findModelForRequest(ModelRequest request) {
        String requestedModelId = request.getModelId();
        if (requestedModelId != null && !requestedModelId.isBlank()) {
            Optional<RegisteredModel> named = getModel(requestedModelId);
            if (named.isEmpty()) {
                LOGGER.warning("Request names model " + requestedModelId + " which is not configured");
                return null;
            }
            if (!named.get().data().supportsRequest(request)) {
                LOGGER.warning("Request names model " + requestedModelId + " which cannot handle the request");
                return null;
            }
            return named.get();
        }
        return models.stream()
                .filter(model -> model.data().supportsRequest(request))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the highest max temperature reported by any configured model that supports a custom temperature.
     *
     * @return null if no such model reports a max temperature, otherwise the highest reported max temperature.
     */
    public @Nullable Double getHighestMaxTemperature() {
        return models.stream()
                .map(RegisteredModel::data)
                .filter(modelData -> modelData.hasCapability(ModelCapability.CUSTOM_TEMPERATURE))
                .map(ModelData::getMaxTemperature)
                .filter(Objects::nonNull)
                .max(Double::compare)
                .orElse(null);
    }
}
