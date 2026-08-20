package io.ohmvir.plugins.jenkinscr.api.client;

import io.ohmvir.plugins.jenkinscr.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinscr.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinscr.configuration.agents.AgentConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class ModelRequest implements Cloneable {
    private final AgentConfiguration agentConfiguration;
    private final HashMap<UUID, byte[]> files = new HashMap<>();
    private final HashMap<UUID, ModelInputType> fileInputTypes = new HashMap<>();
    private @Getter @Setter String promptText = "";
    private @Getter @Setter ModelConversation conversationHistory = null;
    private final Set<ModelOutputType> requestedOutputTypes = new HashSet<>();
    private final Set<ModelInputType> inputTypes = new HashSet<>();

    public ModelRequest(AgentConfiguration agentConfiguration){
        this.agentConfiguration = agentConfiguration;
    }

    public UUID AttachFile(byte[] fileBytes, ModelInputType fileType){
        UUID uuid = UUID.randomUUID();
        files.put(uuid, fileBytes);
        inputTypes.add(fileType);
        fileInputTypes.put(uuid, fileType);
        return uuid;
    }
    public boolean RemoveFile(UUID fileId){
        if(fileInputTypes.containsKey(fileId) && fileInputTypes.values().stream().filter(i -> i.equals(fileInputTypes.get(fileId))).count() > 1){
            inputTypes.remove(fileInputTypes.get(fileId));
        }
        fileInputTypes.remove(fileId);
        return files.remove(fileId) != null;
    }
    public Set<UUID> GetFileIds(){
        return files.keySet();
    }
    /** Appends text to the prompt. */
    public void AttachText(String text){
        if(!text.isEmpty()){
            inputTypes.add(ModelInputType.TEXT);
        }
        promptText += text;
    }

    public Set<ModelInputType> getInputTypes(){
        return Collections.unmodifiableSet(inputTypes);
    }

    public Set<ModelOutputType> getOutputTypes(){
        return Collections.unmodifiableSet(requestedOutputTypes);
    }

    public void RequestOutputType(ModelOutputType outputType){
        requestedOutputTypes.add(outputType);
    }
    public void RemoveOutputTypeRequest(ModelOutputType outputType){
        requestedOutputTypes.remove(outputType);
    }
  //  public abstract void AttachTool(ToolData tool); future TODO
  //  public abstract void AttachSkill(SkillData skill); future TODO

    /**
     * For convenience, generally flow will be to make a base request with all tools, skills and such and then clone it and add
     * the specific prompt text
     * @return A shallow clone of the prompt. File byte data will be copied, but Skills and tool data which are stored in classes will continue to refer to the old data.
     */
    @Override
    public ModelRequest clone() {
        ModelRequest newRequest = new ModelRequest(agentConfiguration);
        newRequest.files.putAll(files);
        newRequest.inputTypes.addAll(inputTypes);
        newRequest.fileInputTypes.putAll(fileInputTypes);
        newRequest.promptText = promptText;
        newRequest.conversationHistory = conversationHistory;
        newRequest.requestedOutputTypes.addAll(requestedOutputTypes);
        return newRequest;
    }
}
