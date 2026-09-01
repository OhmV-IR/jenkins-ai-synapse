package io.ohmvir.plugins.jenkinscr.api.input;

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
    private final Set<ModelOutputType> requestedOutputTypes = new HashSet<>();
    private final Set<ModelInputType> inputTypes = new HashSet<>();
    private @Getter @Setter @Nullable ModelConversation conversationHistory = null;
    private final ArrayList<SkillData> skills = new ArrayList<>();
    private final @Getter List<Tool> tools = new ArrayList<>();
    private @Getter final List<ModelInput> modelInputs = new ArrayList<>();

    public ModelRequest() {}


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
        newRequest.inputTypes.addAll(inputTypes);
        newRequest.conversationHistory = conversationHistory;
        newRequest.requestedOutputTypes.addAll(requestedOutputTypes);
        newRequest.skills.addAll(skills);
        newRequest.modelInputs.addAll(modelInputs);
        return newRequest;
    }
}
