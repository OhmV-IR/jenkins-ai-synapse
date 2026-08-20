package io.ohmvir.plugins.jenkinscr.api.client;

import io.ohmvir.plugins.jenkinscr.api.models.ModelOutputType;
import lombok.Getter;

import java.util.*;

public class ModelResponse {
    private HashMap<UUID, byte[]> fileOutputs = new HashMap<>();
    private HashMap<UUID, ModelOutputType> fileOutputTypes = new HashMap<>();
    private HashSet<ModelOutputType> outputTypes = new HashSet<>();
    private @Getter String responseText = "";
    // TODO add tool calls and other model outputs here.

    public ModelResponse(HashMap<UUID, byte[]> fileOutputs, HashMap<UUID, ModelOutputType> fileOutputTypes,
                         HashSet<ModelOutputType> outputTypes, String responseText) {
        this.fileOutputs = fileOutputs;
        this.fileOutputTypes = fileOutputTypes;
        this.outputTypes = outputTypes;
        this.responseText = responseText;
    }

    public void AddOutputType(ModelOutputType outputType){
        outputTypes.add(outputType);
    }

    public ModelOutputType GetFileOutputType(UUID fileId){
        return fileOutputTypes.get(fileId);
    }

    public void AddFileOutput(UUID outputFileId, byte[] fileOutputData){
        fileOutputs.put(outputFileId, fileOutputData);
    }

    public Map<UUID, byte[]> getFileOutputs() {
        return Collections.unmodifiableMap(fileOutputs);
    }

    public boolean HasOutputType(ModelOutputType outputType){
        return outputTypes.contains(outputType);
    }

    public void PushToConversation(ModelConversation conversation){
        conversation.AddResponse(this);
    }
}
