package io.ohmvir.plugins.jenkinscr.api.client.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ohmvir.plugins.jenkinscr.api.client.ModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.ModelConversation;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.api.client.ModelResponse;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinscr.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.client.OllamaClientConfiguration;
import io.ohmvir.plugins.jenkinscr.configuration.models.OllamaModelConfiguration;
import io.ohmvir.plugins.jenkinscr.utils.SecretsUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.logging.Logger;

public class OllamaModelClient extends ModelClient<OllamaModelConfiguration, OllamaClientConfiguration> {
    private static final String GENERATE_API_SUFFIX = "/api/generate";
    private final String apiBaseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .build();
    private final Logger logger = Logger.getLogger(OllamaModelClient.class.getName());

    public OllamaModelClient(ModelData modelData, OllamaModelConfiguration configuration, OllamaClientConfiguration clientConfiguration) {
        super(modelData, configuration, clientConfiguration);
        apiBaseUrl = SecretsUtils.getSecretText(configuration.apiBaseUrlCredentialId, null);
    }

    private static String thinkingLevelToString(ModelThinkingLevel level){
        return level.toString().toLowerCase();
    }

    @Override
    public @Nullable ModelResponse generateResponse(ModelRequest request) {
        try {
            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("model", modelConfiguration.modelName);
            reqBody.addProperty("stream", false);
            reqBody.addProperty("prompt", request.getPromptText());
            reqBody.addProperty("system", request.getAgentConfiguration().systemPrompt);
            reqBody.addProperty("think", thinkingLevelToString(request.getAgentConfiguration().thinkingLevel)); // TODO check if the model supports thinking and what levels before doing this
            if(clientConfiguration.getKeepAliveSeconds() != 0) {
                reqBody.addProperty("keep_alive", clientConfiguration.getKeepAliveSeconds() + "s");
            }
            JsonObject options = new JsonObject();
            options.addProperty("temperature", request.getAgentConfiguration().temperature);
            reqBody.add("options", options);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + GENERATE_API_SUFFIX))
                    .POST(
                            HttpRequest.BodyPublishers.ofString(reqBody.toString())
                    )
                    .build();
            String resBodyStr = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
            if(resBodyStr == null){
                logger.warning("Failed to generate response using ollama model client as no body was returned from api");
                return null;
            }
            JsonObject resBody = JsonParser.parseString(resBodyStr).getAsJsonObject();
            if(resBody.get("error") != null){
                logger.warning("Failed to generate response using ollama model client with error: " + resBody.get("error").getAsString());
                return null;
            }
            if(!Objects.equals(resBody.get("done_reason").getAsString(), "stop")){
                logger.warning("Failed to generate response as model stopped for a reason other than finishing the task: " + resBody.get("done_reason").getAsString());
                return null;
            }
            ModelResponse modelResponse = new ModelResponse();
            modelResponse.setResponseText(resBody.get("response").getAsString());
            modelResponse.setThinkingText(resBody.get("thinking").getAsString());
            modelResponse.AddOutputType(ModelOutputType.UNSTRUCTURED_TEXT);
            return modelResponse;
        } catch(Exception e) {
            Logger.getLogger(getClass().getName()).severe("OllamaModelClient failed to generate a response to a model request: " + e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public ModelConversation beginConversation() {
        return new ModelConversation();
    }
}
