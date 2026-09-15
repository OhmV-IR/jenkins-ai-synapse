package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputDescriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

public class ModelRequest implements Cloneable {
    private final Set<Class<ModelOutput>> requestedOutputTypes = new HashSet<>();
    private final @Getter List<ModelInput> modelInputs = new ArrayList<>();
    @Setter
    private ModelConversation associatedConversation = null;

    public static Set<ModelInputType> getInputTypesFromClass(Class<? extends ModelInput> inputType) {
        ModelInputDescriptor desc = (ModelInputDescriptor) Jenkins.get().getDescriptor(inputType);
        if (desc == null) {
            throw new IllegalArgumentException(inputType.getName() + " is not a valid input type.");
        }
        return desc.getRequiredInputTypes();
    }

    public static Set<ModelOutputType> getOutputTypesFromClass(Class<? extends ModelOutput> outputType) {
        ModelOutputDescriptor desc = (ModelOutputDescriptor) Jenkins.get().getDescriptor(outputType);
        if (desc == null) {
            throw new IllegalArgumentException(outputType.getName() + " is not a valid output type.");
        }
        return desc.getRequiredOutputTypes();
    }

    public static Set<ModelCapability> getRequiredInputCapabilities(Class<? extends ModelInput> inputType) {
        ModelInputDescriptor desc = (ModelInputDescriptor) Jenkins.get().getDescriptor(inputType);
        if (desc == null) {
            throw new IllegalArgumentException(inputType.getName() + " is not a valid input type.");
        }
        return desc.getRequiredCapabilities();
    }

    public static Set<ModelCapability> getRequiredOutputCapabilities(Class<? extends ModelOutput> outputType) {
        ModelOutputDescriptor desc = (ModelOutputDescriptor) Jenkins.get().getDescriptor(outputType);
        if (desc == null) {
            throw new IllegalArgumentException(outputType.getName() + " is not a valid output type.");
        }
        return desc.getRequiredCapabilities();
    }

    public ModelRequest() {}

    public Set<ModelInputType> getInputTypes() {
        return getInputClasses().stream()
                .map(ModelRequest::getInputTypesFromClass)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<Class<? extends ModelInput>> getInputClasses() {
        return modelInputs.stream().map(ModelInput::getClass).collect(Collectors.toSet());
    }

    public Set<ModelOutputType> getOutputTypes() {
        return requestedOutputTypes.stream()
                .map(ModelRequest::getOutputTypesFromClass)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public Set<ModelCapability> getRequiredCapabilities() {
        Set<ModelCapability> capabilities = modelInputs.stream()
                .map(ModelInput::getClass)
                .map(ModelRequest::getRequiredInputCapabilities)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        capabilities.addAll(requestedOutputTypes.stream()
                .map(ModelRequest::getRequiredOutputCapabilities)
                .flatMap(Set::stream)
                .toList());
        return capabilities;
    }

    public void requestOutputType(Class<ModelOutput> outputType) {
        requestedOutputTypes.add(outputType);
    }

    public void addInput(ModelInput input) {
        modelInputs.add(input);
    }

    public void removeOutputTypeRequest(Class<ModelOutput> outputType) {
        requestedOutputTypes.remove(outputType);
    }

    public void removeOutputTypeRequest(ModelOutputType outputType) {
        requestedOutputTypes.removeIf(requestedOutputType ->
                getOutputTypesFromClass(requestedOutputType).contains(outputType));
    }

    public void attachTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("tool parameter should not be null");
        }
        addInput(new InputToolContent(tool));
    }

    public boolean hasInputType(Class<?> clazz) {
        return modelInputs.stream().anyMatch(clazz::isInstance);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T getInput(Class<T> clazz) {
        return (T) modelInputs.stream().filter(clazz::isInstance).findFirst().get();
    }

    public void attachTool(String toolName) {
        attachTool(ToolRegistry.getTool(toolName));
    }

    public void attachSkill(SkillData skill) {
        addInput(new InputSkillContent(skill));
    }

    public @Nullable ModelResponse execute(){
        return execute(ModelData.createClientForRequest(this));
    }

    public @Nullable ModelResponse execute(ModelClient<?, ?> client){
        try {
            if (client == null) {
                return null;
            }
            List<ModelOutput> currentOutputs = new ArrayList<>();
            List<ModelContent> finalContent = new ArrayList<>(modelInputs);
            while (true) {
                List<ModelOutput> stepOutputs = client.takeStep(this, modelInputs);
                if (stepOutputs.isEmpty()) {
                    break;
                }
                currentOutputs.addAll(stepOutputs);
                finalContent.addAll(stepOutputs);
                List<ModelInput> newInputs = generateNewInputsForOutputs(stepOutputs);
                finalContent.addAll(newInputs);
                modelInputs.addAll(newInputs);
            }
            if (requestedOutputTypes.stream()
                    .allMatch(requestedType ->
                            currentOutputs.stream().anyMatch(requestedType::isInstance)
                    )) {
                return null;
            }
            if (associatedConversation != null) {
                associatedConversation.addAllContent(finalContent);
            }
            return new ModelResponse(currentOutputs);
        } catch(Exception e) {
            Logger.getLogger(ModelRequest.class.getName()).log(Level.WARNING, "Failed to execute ModelRequest", e);
            return null;
        }
    }

    /**
     * Used within request execution to generate all necessary inputs from anything the model outputs. Namely, executing tool calls.
     * @param stepOutputs The outputs to generate inputs for
     * Appends the inputs to the internal model inputs list
     */
    private static List<ModelInput> generateNewInputsForOutputs(List<ModelOutput> stepOutputs) {
        List<ModelInput> newInputs = new ArrayList<>();
        stepOutputs.forEach(modelOutput -> {
            switch(modelOutput){
                case ToolCallContent toolCallOutput -> {
                    Tool tool = ToolRegistry.getTool(toolCallOutput.getName());
                    if(tool == null){
                        Logger.getLogger(ModelResponse.class.getName()).severe("Failed to find tool model tried to call with name " + toolCallOutput.getName());
                        newInputs.add(new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null));
                        return;
                    }
                    Optional<String> toolCallResponseStr = tool.callTool(toolCallOutput.getToolArguments());
                    if(toolCallResponseStr.isEmpty()){
                        Logger.getLogger(ModelResponse.class.getName()).severe("Failed to call model tool with name " + toolCallOutput.getName());
                        newInputs.add(new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null));
                        return;
                    }
                    newInputs.add(new ToolCallResponseContent(toolCallOutput.getToolUseId(), true, toolCallResponseStr.get()));
                }
                default -> {}
            }
        });
        return newInputs;
    }

    /**
     * For convenience, generally flow will be to make a base request with all tools, skills and such and then clone it and add
     * the specific prompt text
     *
     * @return A shallow clone of the prompt. File byte data will be copied, but Skills and tool data which are stored in classes will continue to refer to the old data.
     */
    @Override
    public ModelRequest clone() throws CloneNotSupportedException {
        return (ModelRequest) super.clone();
    }
}
