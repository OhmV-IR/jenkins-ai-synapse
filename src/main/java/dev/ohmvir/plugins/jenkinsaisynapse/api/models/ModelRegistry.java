package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.util.Timer;
import org.jspecify.annotations.Nullable;

/**
 * The configured models, in the user's order of preference (the order on the Models page). Instances are plain Java
 * and can be used in unit tests; {@link #get()} is the one Jenkins keeps up to date with the Models page.
 * <p>
 * Everything a reload produces is published as one immutable {@link Snapshot}. Like
 * {@link dev.ohmvir.plugins.jenkinsaisynapse.utils.SnapshotCache}, each reload takes a generation number when it
 * starts and its snapshot is only installed if no newer generation has been installed, so a slow reload of an older
 * configuration can never overwrite the result of a newer one.
 */
public class ModelRegistry {
    private static final Logger LOGGER = Logger.getLogger(ModelRegistry.class.getName());
    private static final long RETRY_DELAY_SECONDS = 60;
    private static final ModelRegistry GLOBAL = new ModelRegistry(true);

    /**
     * What the registry holds, replaced as a whole so that readers never see a mix of two reloads.
     *
     * @param generation The generation of the reload or {@link #setModels} that made this snapshot.
     * @param models     The registered models in order of preference.
     * @param configured The configurations of the reload, in order, so recovered models can be put back in place.
     * @param failed     The configurations of the reload that could not be described, to be retried.
     * @param providers  The providers the reload used, for retrying.
     */
    private record Snapshot(
            long generation,
            List<RegisteredModel> models,
            List<ModelConfiguration> configured,
            List<ModelConfiguration> failed,
            Collection<? extends ModelProvider<?>> providers) {
        static final Snapshot EMPTY = new Snapshot(0, List.of(), List.of(), List.of(), List.of());
    }

    private final AtomicLong requestedGeneration = new AtomicLong();
    private volatile Snapshot snapshot = Snapshot.EMPTY;
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
                // The generation is taken before reading the configuration, so that a reload of a newer
                // configuration that starts meanwhile always wins over this one.
                long generation = GLOBAL.requestedGeneration.incrementAndGet();
                GLOBAL.reload(generation, ModelsConfiguration.get().getModels(), ModelProvider.all());
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to load models", e);
            }
        });
    }

    /**
     * Replaces the registered models with the given configurations, keeping their order. A configuration that no
     * provider handles or whose model id is already taken is skipped and logged. A configuration that can't be
     * described, for example because its provider is briefly unreachable, is logged and left out for now; the global
     * registry retries those periodically, see {@link #retryFailed}. If another reload started after this one, its
     * result is kept even if this reload finishes last.
     */
    public void reload(
            List<? extends ModelConfiguration> configurations, Collection<? extends ModelProvider<?>> providers) {
        reload(requestedGeneration.incrementAndGet(), configurations, providers);
    }

    private void reload(
            long generation,
            List<? extends ModelConfiguration> configurations,
            Collection<? extends ModelProvider<?>> providers) {
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
        Snapshot fresh = new Snapshot(
                generation, List.copyOf(loaded), List.copyOf(configurations), List.copyOf(notDescribed), providers);
        synchronized (this) {
            if (generation <= snapshot.generation()) {
                LOGGER.info("Dropping the result of a model reload that a newer reload has replaced");
                return;
            }
            snapshot = fresh;
        }
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
        if (retryInBackground && !snapshot.failed().isEmpty() && retryScheduled.compareAndSet(false, true)) {
            Timer.get()
                    .schedule(
                            () -> {
                                retryScheduled.set(false);
                                try {
                                    retryFailed();
                                } catch (RuntimeException e) {
                                    LOGGER.log(Level.WARNING, "Failed to retry loading models", e);
                                }
                            },
                            RETRY_DELAY_SECONDS,
                            TimeUnit.SECONDS);
        }
    }

    /**
     * Describes the models that the last {@link #reload} could not describe, and registers the ones that succeed in
     * their configured place. The global registry does this every minute until nothing is left to retry. If the
     * registry is reloaded or {@link #setModels} is called while this runs, the outcome is dropped since the
     * newer generation made its own list.
     */
    public void retryFailed() {
        Snapshot start = snapshot;
        List<ModelConfiguration> order = start.configured();
        List<ModelConfiguration> pending = start.failed();
        try {
            Map<String, RegisteredModel> recovered = new HashMap<>();
            List<ModelConfiguration> stillFailed = new ArrayList<>();
            for (ModelConfiguration configuration : pending) {
                ModelProvider<?> provider = ModelProvider.forConfiguration(configuration, start.providers());
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
            Map<String, RegisteredModel> byId = new HashMap<>(recovered);
            for (RegisteredModel model : start.models()) {
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
            Snapshot retried = new Snapshot(
                    start.generation(), List.copyOf(merged), order, List.copyOf(stillFailed), start.providers());
            synchronized (this) {
                // Checked and published under the same lock, so a newer snapshot can't be overwritten in between.
                if (snapshot != start) {
                    return;
                }
                snapshot = retried;
            }
        } finally {
            scheduleRetry();
        }
    }

    /**
     * Replaces the registered models. The list order is the order of preference. This counts as the newest reload:
     * it wins over reloads that started earlier and nothing is left to retry.
     */
    public void setModels(List<RegisteredModel> models) {
        List<RegisteredModel> copy = List.copyOf(models);
        List<ModelConfiguration> configurations =
                copy.stream().map(RegisteredModel::configuration).toList();
        long generation = requestedGeneration.incrementAndGet();
        synchronized (this) {
            if (generation > snapshot.generation()) {
                snapshot = new Snapshot(generation, copy, configurations, List.of(), snapshot.providers());
            }
        }
    }

    /**
     * @return The registered models in order of preference.
     */
    public List<RegisteredModel> getModels() {
        return snapshot.models();
    }

    public Optional<RegisteredModel> getModel(String modelId) {
        return snapshot.models().stream()
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
                LOGGER.warning("Request names model " + requestedModelId + " which cannot handle the request: "
                        + String.join("; ", named.get().data().unsupportedReasons(request)));
                return null;
            }
            return named.get();
        }
        return snapshot.models().stream()
                .filter(model -> model.data().supportsRequest(request))
                .findFirst()
                .orElse(null);
    }

    /**
     * Explains why the registered models can't handle a request, one line per model in order of preference.
     *
     * @return For each model, its id followed by what it lacks for the request, empty if no models are registered.
     */
    public List<String> describeUnsupported(ModelRequest request) {
        return snapshot.models().stream()
                .map(model -> model.getModelId() + ": "
                        + String.join("; ", model.data().unsupportedReasons(request)))
                .toList();
    }

    /**
     * Returns the highest max temperature reported by any configured model that supports a custom temperature.
     *
     * @return null if no such model reports a max temperature, otherwise the highest reported max temperature.
     */
    public @Nullable Double getHighestMaxTemperature() {
        return snapshot.models().stream()
                .map(RegisteredModel::data)
                .filter(modelData -> modelData.hasCapability(ModelCapability.CUSTOM_TEMPERATURE))
                .map(ModelData::getMaxTemperature)
                .filter(Objects::nonNull)
                .max(Double::compare)
                .orElse(null);
    }
}
