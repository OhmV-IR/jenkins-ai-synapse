package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.*;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.SnapshotCache;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ModelData implements Describable<ModelData>, ExtensionPoint {
    private static final Logger LOGGER = Logger.getLogger(ModelData.class.getName());
    /**
     * Model metadata keyed by model id, in the order the models are configured.
     * Models whose metadata could not be retrieved are absent and treated as unavailable.
     */
    private static final SnapshotCache<ModelData> MODEL_DATA =
            new SnapshotCache<>("model data", ModelData::retrieveAllModelData);

    private @Getter @Setter String modelId;
    private @Getter @Setter List<ModelCapability> capabilities;
    private @Getter @Setter List<ModelInputType> inputs;
    private @Getter @Setter List<ModelOutputType> outputs;
    private @Getter @Setter List<ModelThinkingLevel> supportedThinkingLevels;
    private @Getter @Setter String providerType;
    private @Getter @Setter @Nullable Long maxInputTokens;
    private @Getter @Setter @Nullable Long maxOutputTokens;
    private @Getter @Setter long contextWindow;
    private @Getter @Setter @Nullable Double maxTemperature;

    public ModelData(ModelConfiguration config) {
        this.modelId = config.getModelId();
        this.providerType = config.getProviderType();
        this.capabilities = new ArrayList<>();
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.supportedThinkingLevels = new ArrayList<>();
        this.contextWindow = 0L;
    }

    /**
     * Fetches model metadata in the background, so a slow or failing provider cannot hold up Jenkins startup.
     */
    @Initializer(after = InitMilestone.PLUGINS_STARTED, fatal = false)
    public static void initializeModelDataCache() {
        LOGGER.info("Prefetching model data in the background");
        MODEL_DATA.reloadInBackground();
    }

    /**
     * Rebuilds the model metadata from the current model configurations and swaps it in.
     * Call this after the model configurations change.
     */
    public static void reload() {
        MODEL_DATA.reload();
    }

    private static Map<String, ModelData> retrieveAllModelData() {
        Map<String, ModelData> modelData = new LinkedHashMap<>();
        for (ModelConfiguration config : ModelsManagementLink.get().getModelConfigurations()) {
            ModelData data = retrieveModelData(config);
            if (data == null) {
                LOGGER.warning("No model data available for " + config.getModelId()
                        + ", the model is unavailable until the model configurations are saved again");
            } else {
                modelData.put(config.getModelId(), data);
            }
        }
        return modelData;
    }

    private static @Nullable ModelData retrieveModelData(ModelConfiguration config) {
        try {
            return ExtensionList.lookup(ModelDataRetriever.class).stream()
                    .map(retriever -> retriever.retrieveFromConfigurationGen(config))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve model data for " + config.getModelId(), e);
            return null;
        }
    }

    /**
     * @return null if the model is not configured or its metadata is unavailable, otherwise the model's metadata.
     */
    public static @Nullable ModelData get(String modelId) {
        return MODEL_DATA.get().get(modelId);
    }

    public static @Nullable ModelClient<?, ?> getClient(@Nullable ModelConfiguration config) {
        if (config == null) {
            return null;
        }
        Optional<ModelClient> client = ExtensionList.lookup(ModelClient.class).stream()
                .filter(obj -> {
                    Type genericSuper = obj.getClass().getGenericSuperclass();
                    if (genericSuper instanceof ParameterizedType parameterizedSuper) {
                        return parameterizedSuper.getActualTypeArguments()[0].equals(config.getClass());
                    } else {
                        return false;
                    }
                })
                .findFirst();
        return client.orElse(null);
    }

    public @Nullable ModelClient<?, ?> getClient() {
        return getClient(getModelConfiguration());
    }

    public @Nullable ModelConfiguration getModelConfiguration() {
        return ModelsManagementLink.get().getModelConfigurations().stream()
                .filter(modelCfg -> Objects.equals(modelCfg.getModelId(), modelId))
                .findFirst()
                .orElse(null);
    }

    public @Nullable ModelClientConfiguration getClientConfiguration() {
        ModelClient<?, ?> client = getClient();
        if (client == null) {
            return null;
        }
        Type genericSuper = client.getClass().getGenericSuperclass();
        if (genericSuper instanceof ParameterizedType parameterizedSuper) {
            return ExtensionList.lookup(ModelClientConfiguration.class).stream()
                    .filter(obj -> obj.getClass().equals(parameterizedSuper.getActualTypeArguments()[1]))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Tries to find a model that will accommodate your request.
     *
     * @param request The model request you want to execute.
     * @return null if no models are capable of fulfilling that request or a model client that can fulfill the request.
     */
    public static @Nullable ModelClient<?, ?> createClientForRequest(ModelRequest request) {
        ModelData supportedModel = ModelData.findModelForRequest(request);
        if (supportedModel == null) {
            return null;
        }
        return supportedModel.getClient();
    }

    public static ListBoxModel getAllModelsListBox() {
        ListBoxModel items = new ListBoxModel();
        for (ModelConfiguration config : ModelsManagementLink.get().getModelConfigurations()) {
            items.add(config.getModelIdDisplayName(), config.getModelId());
        }
        return items;
    }

    /**
     * Returns the highest max temperature reported by any configured model that supports a custom temperature.
     *
     * @return null if no such model reports a max temperature, otherwise the highest reported max temperature.
     */
    public static @Nullable Double getHighestMaxTemperature() {
        return MODEL_DATA.get().values().stream()
                .filter(modelData -> modelData.hasCapability(ModelCapability.CUSTOM_TEMPERATURE))
                .map(ModelData::getMaxTemperature)
                .filter(Objects::nonNull)
                .max(Double::compare)
                .orElse(null);
    }

    public static @Nullable ModelData findModelForRequest(ModelRequest request) {
        Optional<ModelData> supportedModel = MODEL_DATA.get().values().stream()
                .filter(modelData -> modelData.supportsRequest(request))
                .findFirst();
        return supportedModel.orElse(null);
    }

    public boolean supportsRequest(ModelRequest request) {
        Optional<ModelInput> thinkingLevel = request.getModelInputs().stream()
                .filter(modelInput -> modelInput instanceof ThinkingLevelContent)
                .findFirst();
        if (thinkingLevel.isPresent()
                && thinkingLevel.get() instanceof ThinkingLevelContent thinkingLevelContent
                && !supportsThinkingLevel(thinkingLevelContent.getThinkingLevel())) {
            return false;
        }
        if (request.getModelInputs().stream()
                .anyMatch(modelInput -> modelInput instanceof TemperatureContent temperatureContent
                        && !supportsTemperature(temperatureContent.getTemperature()))) {
            return false;
        }
        if (request.getInputTypes().stream().anyMatch(inputType -> !supportsInput(inputType))) {
            return false;
        }
        if (request.getOutputTypes().stream().anyMatch(outputType -> !supportsOutput(outputType))) {
            return false;
        }
        if (request.getRequiredCapabilities().stream().anyMatch(capability -> !hasCapability(capability))) {
            return false;
        }
        return true;
    }

    public boolean hasCapability(ModelCapability capability) {
        return capabilities.contains(capability);
    }

    public boolean supportsInput(ModelInputType input) {
        return inputs.contains(input);
    }

    public boolean supportsOutput(ModelOutputType output) {
        return outputs.contains(output);
    }

    public boolean supportsThinkingLevel(ModelThinkingLevel input) {
        return supportedThinkingLevels.contains(input);
    }

    public boolean supportsTemperature(double temperature) {
        return temperature >= 0 && (maxTemperature == null || temperature <= maxTemperature);
    }

    public boolean supportsThinking() {
        return !supportedThinkingLevels.isEmpty();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ModelData> {
        @Override
        public @NonNull String getDisplayName() {
            return "Model Data";
        }
    }
}
