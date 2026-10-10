package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.NoCapableModelException;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ModelData implements Describable<ModelData>, ExtensionPoint {
    private static final Logger LOGGER = Logger.getLogger(ModelData.class.getName());
    private static final Map<String, ModelData> MODEL_DATA = new ConcurrentHashMap<>();
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

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initializeModelDataCache() {
        LOGGER.info("Prefetching model data");
        for (ModelConfiguration config : ModelsManagementLink.get().getModelConfigurations()) {
            LOGGER.info("Prefetching model data for " + config.getModelId());
            initializeModelDataForConfig(config);
        }
    }

    public static void initializeModelDataForConfig(ModelConfiguration config) {
        if (MODEL_DATA.containsKey(config.getModelId())) {
            return;
        }
        MODEL_DATA.put(
                config.getModelId(),
                ExtensionList.lookup(ModelDataRetriever.class).stream()
                        .map(retriever -> retriever.retrieveFromConfigurationGen(config))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElseThrow());
    }

    public static ModelData get(String modelId) {
        return ModelData.MODEL_DATA.get(modelId);
    }

    /**
     * @return The data of every configured model whose data could be retrieved.
     */
    public static List<ModelData> getAll() {
        return List.copyOf(MODEL_DATA.values());
    }

    public static ModelClient<?, ?> getClient(ModelConfiguration config) {
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

    public ModelClient<?, ?> getClient() {
        ModelConfiguration configuration = getModelConfiguration();
        return configuration == null ? null : getClient(configuration);
    }

    public ModelConfiguration getModelConfiguration() {
        return ModelsManagementLink.get().getModelConfigurations().stream()
                .filter(modelCfg -> Objects.equals(modelCfg.getModelId(), modelId))
                .findFirst()
                .orElse(null);
    }

    public ModelClientConfiguration getClientConfiguration() {
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
        MODEL_DATA.forEach(
                (key, value) -> items.add(key, value.getModelConfiguration().getModelIdDisplayName()));
        return items;
    }

    /**
     * @return The configured models whose data could be retrieved, in the order they are configured.
     */
    public static List<ModelData> getConfiguredModels() {
        return ModelsManagementLink.get().getModelConfigurations().stream()
                .map(configuration -> MODEL_DATA.get(configuration.getModelId()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * @return The first configured model that supports the request, or null if there is none.
     */
    public static @Nullable ModelData findModelForRequest(ModelRequest request) {
        return getConfiguredModels().stream()
                .filter(modelData -> modelData.supportsRequest(request))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds the model to run a request on.
     *
     * @param model The id of the model, its display name or its model name; or null to use the first configured model
     *              that supports the request.
     * @throws NoCapableModelException If the model is not configured or does not support the request, or no model
     * supports the request.
     */
    public static ModelData resolve(@Nullable String model, ModelRequest request) throws NoCapableModelException {
        Map<String, List<String>> unsupported = new LinkedHashMap<>();
        getConfiguredModels()
                .forEach(modelData ->
                        unsupported.put(modelData.getModelId(), modelData.getUnsupportedRequirements(request)));
        if (model == null) {
            ModelData found = findModelForRequest(request);
            if (found == null) {
                throw new NoCapableModelException(
                        "No configured model supports this request. Configured models: " + describe(unsupported),
                        unsupported);
            }
            return found;
        }
        ModelData found = get(model);
        if (found == null) {
            found = ModelsManagementLink.get().getModelConfigurations().stream()
                    .filter(configuration -> model.equals(configuration.getModelIdDisplayName())
                            || model.equals(configuration.getModelName())
                            || model.equals(configuration.getModelDisplayName()))
                    .map(configuration -> MODEL_DATA.get(configuration.getModelId()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (found == null) {
            throw new NoCapableModelException(
                    "Unknown model '" + model + "'. Configured models: " + describe(unsupported), unsupported);
        }
        List<String> missing = found.getUnsupportedRequirements(request);
        if (!missing.isEmpty()) {
            throw new NoCapableModelException(
                    "Model " + found.getModelId() + " does not support " + String.join(", ", missing)
                            + ", which this request needs",
                    Map.of(found.getModelId(), missing));
        }
        return found;
    }

    private static String describe(Map<String, List<String>> unsupported) {
        if (unsupported.isEmpty()) {
            return "(none)";
        }
        StringJoiner joiner = new StringJoiner(", ");
        unsupported.forEach(
                (id, missing) -> joiner.add(missing.isEmpty() ? id : id + " (no " + String.join(", ", missing) + ")"));
        return joiner.toString();
    }

    public boolean supportsRequest(ModelRequest request) {
        return getUnsupportedRequirements(request).isEmpty();
    }

    /**
     * @return A description of every requirement of the request this model does not meet, empty if it supports the
     * request.
     */
    public List<String> getUnsupportedRequirements(ModelRequest request) {
        List<String> unsupported = new ArrayList<>();
        ModelThinkingLevel thinkingLevel = request.getParams().getThinkingLevel();
        if (thinkingLevel != null && !supportsThinkingLevel(thinkingLevel)) {
            unsupported.add("thinking level " + thinkingLevel);
        }
        request.getInputTypes().stream()
                .filter(inputType -> !supportsInput(inputType))
                .forEach(inputType -> unsupported.add("input type " + inputType));
        request.getOutputTypes().stream()
                .filter(outputType -> !supportsOutput(outputType))
                .forEach(outputType -> unsupported.add("output type " + outputType));
        request.getRequiredCapabilities().stream()
                .filter(capability -> !hasCapability(capability))
                .forEach(capability -> unsupported.add("capability " + capability));
        return unsupported;
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
