package io.ohmvir.plugins.jenkinscr.api.models.retrievers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ohmvir.plugins.jenkinscr.api.models.*;
import io.ohmvir.plugins.jenkinscr.configuration.models.OllamaModelConfiguration;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OllamaModelDataRetriever extends ModelDataRetriever<OllamaModelConfiguration> {
    private static final String RETRIEVE_MODEL_INFO_SUFFIX = "/api/show";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public OllamaModelDataRetriever() {
        super(OllamaModelConfiguration.class);
    }

    @Override
    public ModelData retrieveFromConfiguration(OllamaModelConfiguration configuration) throws IOException, InterruptedException {
        HttpRequest modelDetailsReq = HttpRequest.newBuilder()
                .uri(URI.create(
                        SecretsUtils.getSecretText(configuration.apiBaseUrlCredentialId, null) + RETRIEVE_MODEL_INFO_SUFFIX))
                .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"model\":\"%s\"\"verbose\":true}", configuration.modelName)))
                .build();
        HttpResponse<String> res = httpClient.send(modelDetailsReq, HttpResponse.BodyHandlers.ofString());
        JsonObject resJson = JsonParser.parseString(res.body()).getAsJsonObject();
        Set<String> capabilities = resJson.getAsJsonArray("capabilities").asList()
                .stream().map(JsonElement::getAsString)
                .collect(Collectors.toSet());
        ModelData ret = new ModelData();
        if (configuration.modelName.contains("gpt-oss")) {
            ret.setSupportedThinkingLevels(List.of(ModelThinkingLevel.LOW, ModelThinkingLevel.MEDIUM, ModelThinkingLevel.HIGH));
        } else if (capabilities.contains("thinking")) {
            ret.setSupportedThinkingLevels(List.of(ModelThinkingLevel.OFF, ModelThinkingLevel.LOW, ModelThinkingLevel.MEDIUM, ModelThinkingLevel.HIGH, ModelThinkingLevel.MAX));
        }
        ArrayList<ModelCapability> capabilitiesEnumArr = new ArrayList<>();
        capabilitiesEnumArr.add(ModelCapability.STREAMING);
        if (capabilities.contains("tools")) {
            capabilitiesEnumArr.add(ModelCapability.TOOLS);
        }
        ArrayList<ModelInputType> supportedInputTypes = new ArrayList<>();
        if (capabilities.contains("audio")) {
            supportedInputTypes.add(ModelInputType.AUDIO);
        }
        if (capabilities.contains("vision")) {
            supportedInputTypes.add(ModelInputType.IMAGE);
            supportedInputTypes.add(ModelInputType.VIDEO); // Technically supported by passing multiple frames to the model.
        }
        ret.setInputs(supportedInputTypes);
        ret.setCapabilities(capabilitiesEnumArr);
        ArrayList<ModelOutputType> outputTypes = new ArrayList<>();
        outputTypes.add(ModelOutputType.UNSTRUCTURED_TEXT);
        outputTypes.add(ModelOutputType.STRUCTURED_OUTPUT);
        if (capabilities.contains("embeddings")) {
            outputTypes.add(ModelOutputType.EMBEDDINGS);
        }
        ret.setOutputs(outputTypes);
        ret.setProviderType(ModelProviderType.OLLAMA);
        ret.setMaxTemperature(2.0d);
        return ret;
    }
}
