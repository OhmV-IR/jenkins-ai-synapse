package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputKind;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

public class ModelRequest implements Cloneable {
    private static final Logger LOGGER = Logger.getLogger(ModelRequest.class.getName());

    private final Set<ModelOutputKind<?>> requestedOutputs = new LinkedHashSet<>();
    private final Set<ModelCapability> explicitCapabilities = new LinkedHashSet<>();
    private final @Getter List<ModelInput> modelInputs = new ArrayList<>();

    /**
     * The id of the model to run this request on, or null to pick the first suitable model in the user's order of
     * preference.
     */
    private @Getter @Setter @Nullable String modelId;

    @Setter
    private ModelConversation associatedConversation = null;

    public ModelRequest() {}

    public Set<ModelInputType> getInputTypes() {
        return modelInputs.stream()
                .map(ModelInput::getRequiredInputTypes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public Set<ModelOutputType> getOutputTypes() {
        return requestedOutputs.stream()
                .map(ModelOutputKind::requiredOutputTypes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public Set<ModelOutputKind<?>> getRequestedOutputs() {
        return Collections.unmodifiableSet(requestedOutputs);
    }

    public Set<ModelCapability> getRequiredCapabilities() {
        Set<ModelCapability> capabilities = modelInputs.stream()
                .map(ModelInput::getRequiredCapabilities)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        requestedOutputs.forEach(kind -> capabilities.addAll(kind.requiredCapabilities()));
        capabilities.addAll(explicitCapabilities);
        return capabilities;
    }

    /**
     * Requires the model to have a capability that no input or requested output implies, eg a provider-defined
     * capability such as {@code ModelCapability.of("vendor:computer-use")}.
     */
    public void requireCapability(ModelCapability capability) {
        explicitCapabilities.add(Objects.requireNonNull(capability, "capability"));
    }

    /**
     * Requires the response to contain at least one output of the given kind, eg {@code OutputImageContent.KIND}.
     */
    public void requestOutput(ModelOutputKind<?> outputKind) {
        requestedOutputs.add(Objects.requireNonNull(outputKind, "outputKind"));
    }

    public void removeOutputRequest(ModelOutputKind<?> outputKind) {
        requestedOutputs.remove(outputKind);
    }

    public void removeOutputTypeRequest(ModelOutputType outputType) {
        requestedOutputs.removeIf(kind -> kind.requiredOutputTypes().contains(outputType));
    }

    public void addInput(ModelInput input) {
        modelInputs.add(input);
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

    public <T> @Nullable T getInput(Class<T> clazz) {
        return modelInputs.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    public void attachTool(String toolName) {
        attachTool(ToolRegistry.getTool(toolName));
    }

    public void attachSkill(SkillData skill) {
        addInput(new InputSkillContent(skill));
    }

    /**
     * Runs the request on the model named by {@link #getModelId()}, or on the first suitable configured model.
     */
    public @Nullable ModelResponse execute() {
        return execute(ModelRegistry.get());
    }

    public @Nullable ModelResponse execute(ModelRegistry registry) {
        RegisteredModel model = registry.findModelForRequest(this);
        if (model == null) {
            LOGGER.warning(
                    modelId != null
                            ? "Model " + modelId + " is not configured or cannot handle the request"
                            : "No configured model can handle the request");
            return null;
        }
        return execute(model);
    }

    public @Nullable ModelResponse execute(RegisteredModel model) {
        try {
            ModelClient client = model.createClient();
            List<ModelInput> turnInputs = new ArrayList<>(modelInputs);
            List<ModelOutput> outputs = new ArrayList<>();
            List<ModelContent> finalContent = new ArrayList<>(modelInputs);
            while (true) {
                List<ModelOutput> stepOutputs = client.takeStep(model.data(), this, List.copyOf(turnInputs));
                outputs.addAll(stepOutputs);
                finalContent.addAll(stepOutputs);
                List<ModelInput> newInputs = generateNewInputsForOutputs(stepOutputs);
                // The model only needs another step when it is waiting on our tool call responses
                if (newInputs.isEmpty()) {
                    break;
                }
                finalContent.addAll(newInputs);
                turnInputs.addAll(newInputs);
            }
            Optional<ModelOutputKind<?>> missingOutput = requestedOutputs.stream()
                    .filter(kind -> outputs.stream().noneMatch(kind::isKindOf))
                    .findFirst();
            if (missingOutput.isPresent()) {
                LOGGER.warning("Model " + model.getModelId() + " did not produce requested output "
                        + missingOutput.get().type().getName());
                return null;
            }
            if (associatedConversation != null) {
                associatedConversation.addAllContent(finalContent);
            }
            return new ModelResponse(outputs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while executing ModelRequest", e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to execute ModelRequest", e);
            return null;
        }
    }

    /**
     * Used within request execution to generate all necessary inputs from anything the model outputs. Namely, executing tool calls.
     * @param stepOutputs The outputs to generate inputs for
     * @return The inputs to send back to the model
     */
    private static List<ModelInput> generateNewInputsForOutputs(List<ModelOutput> stepOutputs) {
        List<ModelInput> newInputs = new ArrayList<>();
        stepOutputs.forEach(modelOutput -> {
            if (modelOutput instanceof ToolCallContent toolCallOutput) {
                Tool tool = ToolRegistry.getTool(toolCallOutput.getName());
                if (tool == null) {
                    LOGGER.severe("Failed to find tool model tried to call with name " + toolCallOutput.getName());
                    newInputs.add(new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null, null));
                    return;
                }
                Optional<String> toolCallResponseStr = tool.callTool(toolCallOutput.getToolArguments());
                if (toolCallResponseStr.isEmpty()) {
                    LOGGER.severe("Failed to call model tool with name " + toolCallOutput.getName());
                    newInputs.add(new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null, tool));
                    return;
                }
                newInputs.add(new ToolCallResponseContent(
                        toolCallOutput.getToolUseId(), true, toolCallResponseStr.get(), tool));
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
