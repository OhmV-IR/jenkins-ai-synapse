package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.util.Timer;
import org.jspecify.annotations.Nullable;

/**
 * The configured models, in the user's order of preference (the order on the Models page). Instances are plain Java
 * and can be used in unit tests; {@link #get()} is the one Jenkins keeps up to date with the Models page.
 */
public class ModelRegistry {
    private static final Logger LOGGER = Logger.getLogger(ModelRegistry.class.getName());
    private static final long RETRY_DELAY_SECONDS = 60;
    private static final ModelRegistry GLOBAL = new ModelRegistry(true);

    private volatile List<RegisteredModel> models = List.of();
    /** The configurations of the last {@link #reload}, in order, so recovered models can be put back in place. */
    private volatile List<ModelConfiguration> configured = List.of();
    /** The configurations of the last {@link #reload} that could not be described, to be retried. */
    private volatile List<ModelConfiguration> failed = List.of();

    private volatile Collection<? extends ModelProvider<?>> providers = List.of();
    private final boolean retryInBackground;
    private final AtomicBoolean retryScheduled = new AtomicBoolean();

    public ModelRegistry() {
        this(false);
    }

    private ModelRegistry(boolean retryInBackground) {
        this.retryInBackground = retryInBackground;
    }

    public static ModelRegistry get() {
        return GLOBAL;
    }

    /**
     * Describing a model may call the provider's API, so the registry is loaded in the background rather than
     * delaying Jenkins startup, and a failure here must not fail startup either. The registry is empty until the
     * load is done.
     */
    @Initializer(after = InitMilestone.PLUGINS_STARTED, fatal = false)
    public static void initialize() {
        Timer.get().execute(() -> {
            try {
                LOGGER.info("Prefetching model data");
                GLOBAL.reload(ModelsConfiguration.get().getModels(), ModelProvider.all());
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to load models", e);
            }
        });
    }

    /**
     * Replaces the registered models with the given configurations, keeping their order. A configuration that no
     * provider handles or whose model id is already taken is skipped and logged. A configuration that can't be
     * described, for example because its provider is briefly unreachable, is logged and left out for now; the global
     * registry retries those periodically, see {@link #retryFailed}.
     */
    public void reload(
            List<? extends ModelConfiguration> configurations, Collection<? extends ModelProvider<?>> providers) {
        List<RegisteredModel> loaded = new ArrayList<>();
        List<ModelConfiguration> notDescribed = new ArrayList<>();
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
                RegisteredModel model = describe(configuration, provider);
                if (model == null) {
                    notDescribed.add(configuration);
                } else {
                    loaded.add(model);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interrupted while getting model data for " + modelId, e);
                break;
            }
        }
        this.providers = providers;
        configured = List.copyOf(configurations);
        failed = List.copyOf(notDescribed);
        models = List.copyOf(loaded);
        scheduleRetry();
    }

    /**
     * @return null if the model can't be described, after logging why.
     */
    private static @Nullable RegisteredModel describe(ModelConfiguration configuration, ModelProvider<?> provider)
            throws InterruptedException {
        String modelId = configuration.getModelId();
        try {
            ModelData data = provider.describeModel(configuration);
            if (data == null) {
                LOGGER.severe("Model provider " + provider.getClass().getName() + " returned no data for " + modelId);
                return null;
            }
            return new RegisteredModel(configuration, provider, data);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get model data for " + modelId, e);
            return null;
        }
    }

    /**
     * Models that failed to describe are retried on a timer, not when a request names them: a request would have to
     * wait for the provider call, while a retry in the background keeps requests fast. Only the global registry
     * retries, so that registries made in unit tests don't start timers.
     */
    private void scheduleRetry() {
        if (retryInBackground && !failed.isEmpty() && retryScheduled.compareAndSet(false, true)) {
            Timer.get().schedule(() -> {
                retryScheduled.set(false);
                try {
                    retryFailed();
                } catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING, "Failed to retry loading models", e);
                }
            }, RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * Describes the models that the last {@link #reload} could not describe, and registers the ones that succeed in
     * their configured place. The global registry does this every minute until nothing is left to retry. If the
     * registry is reloaded while this runs, the outcome is dropped since the new reload made its own list.
     */
    public void retryFailed() {
        List<ModelConfiguration> order = configured;
        List<ModelConfiguration> pending = failed;
        try {
            Map<String, RegisteredModel> recovered = new HashMap<>();
            List<ModelConfiguration> stillFailed = new ArrayList<>();
            for (ModelConfiguration configuration : pending) {
                ModelProvider<?> provider = ModelProvider.forConfiguration(configuration, providers);
                if (provider == null) {
                    continue;
                }
                try {
                    RegisteredModel model = describe(configuration, provider);
                    if (model == null) {
                        stillFailed.add(configuration);
                    } else {
                        recovered.put(model.getModelId(), model);
                        LOGGER.info("Loaded model data for " + model.getModelId() + " on retry");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.log(
                            Level.WARNING,
                            "Interrupted while retrying model data for " + configuration.getModelId(),
                            e);
                    stillFailed.addAll(pending.subList(pending.indexOf(configuration), pending.size()));
                    break;
                }
            }
            if (order != configured) {
                return;
            }
            Map<String, RegisteredModel> byId = new HashMap<>(recovered);
            for (RegisteredModel model : models) {
                byId.put(model.getModelId(), model);
            }
            List<RegisteredModel> merged = new ArrayList<>();
            Set<String> addedIds = new HashSet<>();
            for (ModelConfiguration configuration : order) {
                RegisteredModel model = byId.get(configuration.getModelId());
                if (model != null && addedIds.add(model.getModelId())) {
                    merged.add(model);
                }
            }
            models = List.copyOf(merged);
            failed = List.copyOf(stillFailed);
        } finally {
            scheduleRetry();
        }
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
