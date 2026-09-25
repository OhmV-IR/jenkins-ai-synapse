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
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ModelData implements Describable<ModelData>, ExtensionPoint {
    private static final Logger LOGGER = Logger.getLogger(ModelData.class.getName());
    private static final HashMap<String, ModelData> MODEL_DATA = new HashMap<>();
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
        return getClient(getModelConfiguration());
    }

    public ModelConfiguration getModelConfiguration() {
        return ModelsManagementLink.get().getModelConfigurations().stream()
                .filter(modelCfg -> Objects.equals(modelCfg.getModelId(), modelId))
                .findFirst()
                .orElse(null);
    }

    public ModelClientConfiguration getClientConfiguration() {
        Type genericSuper = getClient().getClass().getGenericSuperclass();
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

    public static @Nullable ModelData findModelForRequest(ModelRequest request) {
        Optional<ModelData> supportedModel = MODEL_DATA.values().stream()
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
