package io.ohmvir.plugins.jenkinscr.api.client;

import io.ohmvir.plugins.jenkinscr.api.models.ModelOutputType;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class ModelResponse {
    private final HashMap<UUID, byte[]> fileOutputs = new HashMap<>();
    private final HashMap<UUID, ModelOutputType> fileOutputTypes = new HashMap<>();
    private final HashSet<ModelOutputType> outputTypes = new HashSet<>();
    private @Getter @Setter String responseText = "";
    private @Getter @Setter String thinkingText = "";
    // TODO add tool calls and other model outputs here.

    public ModelResponse(){

    }

    public void AddOutputType(ModelOutputType outputType) {
        outputTypes.add(outputType);
    }

    public ModelOutputType GetFileOutputType(UUID fileId) {
        return fileOutputTypes.get(fileId);
    }

    public void AddFileOutput(UUID outputFileId, byte[] fileOutputData) {
        fileOutputs.put(outputFileId, fileOutputData);
    }

    public Map<UUID, byte[]> getFileOutputs() {
        return Collections.unmodifiableMap(fileOutputs);
    }

    public boolean HasOutputType(ModelOutputType outputType) {
        return outputTypes.contains(outputType);
    }

    public void PushToConversation(ModelConversation conversation) {
        conversation.AddResponse(this);
    }
}
