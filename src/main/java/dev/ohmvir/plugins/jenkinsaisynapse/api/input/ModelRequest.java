package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestException;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestListener;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputKind;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

public class ModelRequest {
    private static final Logger LOGGER = Logger.getLogger(ModelRequest.class.getName());

    private final Set<ModelOutputKind<?>> requestedOutputs = new LinkedHashSet<>();
    private final Set<ModelCapability> explicitCapabilities = new LinkedHashSet<>();
    private final @Getter List<ModelInput> modelInputs = new ArrayList<>();
    private final Set<String> expandedSkillsAllowedTools = new LinkedHashSet<>();
    private @Getter @Setter @Nullable String modelId;
    @Setter
    private @Nullable ModelConversation associatedConversation;

    public ModelRequest() {}

    /**
     * Copies another request. The copy owns its own input, output, capability and skill-tool collections, so changing
     * one request never changes the other. The inputs themselves and the associated conversation are shared.
     */
    public ModelRequest(ModelRequest other) {
        Objects.requireNonNull(other, "other");
        requestedOutputs.addAll(other.requestedOutputs);
        explicitCapabilities.addAll(other.explicitCapabilities);
        modelInputs.addAll(other.modelInputs);
        expandedSkillsAllowedTools.addAll(other.expandedSkillsAllowedTools);
        modelId = other.modelId;
        associatedConversation = other.associatedConversation;
    }

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
                .collect(Collectors.toCollection(LinkedHashSet::new));
        requestedOutputs.forEach(kind -> capabilities.addAll(kind.requiredCapabilities()));
        capabilities.addAll(explicitCapabilities);
        return capabilities;
    }

    public void requireCapability(ModelCapability capability) {
        explicitCapabilities.add(Objects.requireNonNull(capability, "capability"));
    }

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
        modelInputs.add(Objects.requireNonNull(input, "input"));
    }

    public void attachTool(Tool tool) {
        addInput(new InputToolContent(Objects.requireNonNull(tool, "tool")));
    }

    public void attachTool(String toolName) {
        attachTool(dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry.getTool(toolName));
    }

    public void attachSkill(SkillData skill) {
        addInput(new InputSkillContent(Objects.requireNonNull(skill, "skill")));
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

    /**
     * Converts attached skills to a system prompt and the built-in skill-file tool. This prepares a fresh request
     * before model selection and keeps the skill tool restrictions attached to this request. Calling it again once the
     * skills are expanded has no effect.
     */
    public void expandSkills() {
        List<SkillData> skills = modelInputs.stream()
                .filter(InputSkillContent.class::isInstance)
                .map(input -> ((InputSkillContent) input).getSkill())
                .distinct()
                .toList();
        if (skills.isEmpty()) {
            return;
        }
        skills.stream()
                .map(SkillData::getAllowedTools)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(name -> !name.isBlank())
                .forEach(expandedSkillsAllowedTools::add);
        modelInputs.removeIf(InputSkillContent.class::isInstance);
        String skillPrompt = ReadSkillFileTool.buildSkillsSystemPrompt(skills);
        Optional<ModelInput> systemPrompt = modelInputs.stream()
                .filter(SystemPromptContent.class::isInstance)
                .findFirst();
        if (systemPrompt.isPresent()) {
            String existing = ((SystemPromptContent) systemPrompt.get()).getSystemPrompt();
            String combined = existing == null || existing.isBlank() ? skillPrompt : existing + "\n\n" + skillPrompt;
            modelInputs.set(modelInputs.indexOf(systemPrompt.get()), new SystemPromptContent(combined));
        } else {
            modelInputs.add(0, new SystemPromptContent(skillPrompt));
        }
        boolean hasSkillTool = modelInputs.stream()
                .filter(InputToolContent.class::isInstance)
                .map(input -> ((InputToolContent) input).getTool())
                .anyMatch(tool -> ReadSkillFileTool.NAME.equals(tool.getName()));
        if (!hasSkillTool) {
            attachTool(ReadSkillFileTool.get());
        }
    }

    /**
     * The tools attached to this request, restricted by any {@code allowed-tools} declarations on its skills.
     */
    public Map<String, Tool> getAvailableTools() {
        Map<String, Tool> tools = new LinkedHashMap<>();
        Set<String> allowedTools = new LinkedHashSet<>(expandedSkillsAllowedTools);
        for (ModelInput input : modelInputs) {
            if (input instanceof InputToolContent toolInput && toolInput.getTool() != null) {
                tools.putIfAbsent(toolInput.getTool().getName(), toolInput.getTool());
            } else if (input instanceof InputSkillContent skillInput && skillInput.getSkill() != null) {
                List<String> skillTools = skillInput.getSkill().getAllowedTools();
                if (skillTools != null) {
                    skillTools.stream().filter(name -> !name.isBlank()).forEach(allowedTools::add);
                }
            }
        }
        if (!allowedTools.isEmpty()) {
            allowedTools.add(ReadSkillFileTool.NAME);
            tools.keySet().retainAll(allowedTools);
        }
        return Collections.unmodifiableMap(tools);
    }

    public @Nullable ModelResponse execute() {
        return execute(ModelRegistry.get());
    }

    public @Nullable ModelResponse execute(ModelRegistry registry) {
        // Expanded first so models are selected by what the client receives, not by the SKILLS capability
        expandSkills();
        RegisteredModel model = registry.findModelForRequest(this);
        if (model == null) {
            return null;
        }
        return execute(model);
    }

    public @Nullable ModelResponse execute(RegisteredModel model) {
        try {
            return executeOrThrow(
                    model,
                    ModelRequestListener.NOOP,
                    (tool, toolCall) -> tool.callTool(toolCall));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while executing ModelRequest", e);
            return null;
        } catch (ModelRequestException e) {
            LOGGER.log(Level.WARNING, "Failed to execute ModelRequest", e);
            return null;
        }
    }

    /**
     * Executes this request and runs only tool calls explicitly attached to it (and permitted by its skills).
     * Callers in Jenkins should provide a tool executor that supplies the build's authentication and a listener that
     * writes progress to the build log.
     */
    public ModelResponse executeOrThrow(
            RegisteredModel model,
            ModelRequestListener listener,
            BiFunction<Tool, ToolCallContent, ToolCallResponseContent> toolExecutor)
            throws ModelRequestException, InterruptedException {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(toolExecutor, "toolExecutor");
        expandSkills();
        ModelClient client = model.createClient();
        if (client == null) {
            throw new ModelRequestException("No client is available for model " + model.getModelId());
        }

        List<ModelInput> turnInputs = new ArrayList<>(modelInputs);
        List<ModelContent> finalContent = new ArrayList<>(modelInputs);
        List<ModelOutput> outputs = new ArrayList<>();
        Map<String, Tool> availableTools = getAvailableTools();
        try {
            while (true) {
                List<ModelOutput> stepOutputs = client.takeStep(model.data(), this, List.copyOf(turnInputs), listener);
                if (stepOutputs == null) {
                    throw new ModelRequestException("Model client returned no outputs");
                }
                outputs.addAll(stepOutputs);
                finalContent.addAll(stepOutputs);
                List<ModelInput> toolResponses = new ArrayList<>();
                for (ModelOutput output : stepOutputs) {
                    if (!(output instanceof ToolCallContent toolCall)) {
                        continue;
                    }
                    Tool tool = availableTools.get(toolCall.getName());
                    ToolCallResponseContent response;
                    if (tool == null) {
                        LOGGER.warning("Rejected call to tool '" + toolCall.getName() + "' not attached to the request");
                        response = ToolCallResponseContent.error(
                                toolCall.getToolUseId(), null, "That tool is not available to this request.");
                    } else {
                        response = toolExecutor.apply(tool, toolCall);
                        if (response == null) {
                            throw new ModelRequestException("Tool executor returned no response for " + tool.getName());
                        }
                    }
                    listener.onToolResult(response);
                    toolResponses.add(response);
                }
                if (toolResponses.isEmpty()) {
                    break;
                }
                finalContent.addAll(toolResponses);
                turnInputs.addAll(toolResponses);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ModelRequestException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ModelRequestException("Failed to execute request on model " + model.getModelId(), e);
        }

        Optional<ModelOutputKind<?>> missingOutput = requestedOutputs.stream()
                .filter(kind -> outputs.stream().noneMatch(kind::isKindOf))
                .findFirst();
        if (missingOutput.isPresent()) {
            throw new ModelRequestException("Model " + model.getModelId() + " did not produce requested output "
                    + missingOutput.get().type().getName());
        }
        if (associatedConversation != null) {
            associatedConversation.addAllContent(finalContent);
        }
        return new ModelResponse(outputs);
    }
}
