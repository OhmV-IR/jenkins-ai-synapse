package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import hudson.ExtensionList;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClientFactory;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.*;
import lombok.Getter;
import lombok.Setter;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class ModelData {
    private static final Logger LOGGER = Logger.getLogger(ModelData.class.getName());
    private static final HashMap<String, ModelData> MODEL_DATA = new HashMap<>();
    private @Getter
    @Setter String modelId;
    private @Getter
    @Setter List<ModelCapability> capabilities;
    private @Getter
    @Setter List<ModelInputType> inputs;
    private @Getter
    @Setter List<ModelOutputType> outputs;
    private @Getter
    @Setter List<ModelThinkingLevel> supportedThinkingLevels;
    private @Getter
    @Setter ModelProviderType providerType;
    private @Getter
    @Setter
    @Nullable Long maxInputTokens;
    private @Getter
    @Setter
    @Nullable Long maxOutputTokens;
    private @Getter
    @Setter long contextWindow;
    private @Getter
    @Setter
    @Nullable Double maxTemperature;

    public ModelData() {

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
        MODEL_DATA.put(config.getModelId(), ExtensionList.lookup(ModelDataRetriever.class).stream()
                .map(retriever -> retriever.retrieveFromConfigurationGen(config))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow());
    }

    public static ModelData get(String modelId) {
        return ModelData.MODEL_DATA.get(modelId);
    }

    @SuppressWarnings("unchecked")
    public static ModelClient<?, ?> createClient(ModelConfiguration config) {
        Optional<ModelClientFactory> factory = ExtensionList.lookup(ModelClientFactory.class).stream()
                .filter(obj -> {
                    Type genericSuper = obj.getClass().getGenericSuperclass();
                    if(genericSuper instanceof ParameterizedType parameterizedSuper){
                        return parameterizedSuper.getActualTypeArguments()[0].equals(config.getClass());
                    } else {
                        return false;
                    }
                }).findFirst();
        if(factory.isEmpty()){
            return null;
        }
        Type genericSuper = factory.getClass().getGenericSuperclass();
        if(genericSuper instanceof ParameterizedType parameterizedSuper){
            return factory.get().createClient(get(config.getModelId()), config, ExtensionList.lookupSingleton(parameterizedSuper.getActualTypeArguments()[1].getClass()));
        }
        return null;
    }

    public ModelClient<?, ?> createClient() {
        ModelConfiguration modelConfiguration = ModelsManagementLink.get().getModelConfigurations().stream().filter(modelCfg -> Objects.equals(modelCfg.getModelId(), modelId)).findFirst().get();
        return createClient(modelConfiguration);
    }

    /**
     * Tries to find a model that will accommodate your request.
     *
     * @param request The model request you want to execute.
     * @return null if no models are capable of fulfilling that request or a model client that can fulfill the request.
     */
    public static @Nullable ModelClient<?, ?> createClientForRequest(ModelRequest request) {
        return null; // TODO
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
}
