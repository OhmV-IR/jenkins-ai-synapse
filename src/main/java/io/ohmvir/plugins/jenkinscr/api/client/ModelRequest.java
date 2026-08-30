package io.ohmvir.plugins.jenkinscr.api.client;

import io.ohmvir.plugins.jenkinscr.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinscr.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinscr.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import io.ohmvir.plugins.jenkinscr.api.tools.Tool;
import io.ohmvir.plugins.jenkinscr.api.tools.ToolRegistry;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.*;

public class ModelRequest implements Cloneable {
    private final HashMap<UUID, byte[]> files = new HashMap<>();
    private final HashMap<UUID, ModelInputType> fileInputTypes = new HashMap<>();
    private final Set<ModelOutputType> requestedOutputTypes = new HashSet<>();
    private final Set<ModelInputType> inputTypes = new HashSet<>();
    private @Getter @Setter String promptText = "";
    private @Getter @Setter @Nullable ModelConversation conversationHistory = null;
    private final ArrayList<SkillData> skills = new ArrayList<>();
    private final @Getter List<Tool> tools = new ArrayList<>();
    private @Getter @Setter String systemPrompt = "";
    private @Getter @Setter @Nullable Double temperature = null;
    private @Getter @Setter @Nullable Long maxOutputTokensCount = null;
    private @Getter @Setter ModelThinkingLevel thinkingLevel = ModelThinkingLevel.OFF;

    public ModelRequest() {}

    public UUID AttachFile(byte[] fileBytes, ModelInputType fileType) {
        UUID uuid = UUID.randomUUID();
        files.put(uuid, fileBytes);
        inputTypes.add(fileType);
        fileInputTypes.put(uuid, fileType);
        return uuid;
    }

    public boolean RemoveFile(UUID fileId) {
        if (fileInputTypes.containsKey(fileId) && fileInputTypes.values().stream().filter(i -> i.equals(fileInputTypes.get(fileId))).count() > 1) {
            inputTypes.remove(fileInputTypes.get(fileId));
        }
        fileInputTypes.remove(fileId);
        return files.remove(fileId) != null;
    }

    public Set<UUID> GetFileIds() {
        return files.keySet();
    }

    /**
     * Appends text to the prompt.
     */
    public void AttachText(String text) {
        if (!text.isEmpty()) {
            inputTypes.add(ModelInputType.TEXT);
        }
        promptText += text;
    }

    public Set<ModelInputType> getInputTypes() {
        return Collections.unmodifiableSet(inputTypes);
    }

    public Set<ModelOutputType> getOutputTypes() {
        return Collections.unmodifiableSet(requestedOutputTypes);
    }

    public void RequestOutputType(ModelOutputType outputType) {
        requestedOutputTypes.add(outputType);
    }

    public void RemoveOutputTypeRequest(ModelOutputType outputType) {
        requestedOutputTypes.remove(outputType);
    }
    public void AttachTool(Tool tool){
        if(tool == null){
            throw new IllegalArgumentException("tool parameter should not be null");
        }
        tools.add(tool);
    }
    public void AttachTool(String toolName){
        AttachTool(ToolRegistry.getTool(toolName));
    }
    public void AttachSkill(SkillData skill){
        skills.add(skill);
    }

    public List<SkillData> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    /**
     * For convenience, generally flow will be to make a base request with all tools, skills and such and then clone it and add
     * the specific prompt text
     *
     * @return A shallow clone of the prompt. File byte data will be copied, but Skills and tool data which are stored in classes will continue to refer to the old data.
     */
    @Override
    public ModelRequest clone() {
        ModelRequest newRequest = new ModelRequest();
        newRequest.maxOutputTokensCount = maxOutputTokensCount;
        newRequest.temperature = temperature;
        newRequest.systemPrompt = systemPrompt;
        newRequest.thinkingLevel = thinkingLevel;
        newRequest.files.putAll(files);
        newRequest.inputTypes.addAll(inputTypes);
        newRequest.fileInputTypes.putAll(fileInputTypes);
        newRequest.promptText = promptText;
        newRequest.conversationHistory = conversationHistory;
        newRequest.requestedOutputTypes.addAll(requestedOutputTypes);
        newRequest.skills.addAll(skills);
        return newRequest;
    }
}
