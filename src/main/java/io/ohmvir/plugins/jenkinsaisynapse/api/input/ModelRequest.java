package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestException;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestListener;
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
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

public class ModelRequest implements Cloneable, Describable<ModelRequest>, ExtensionPoint {
    private static final Logger LOGGER = Logger.getLogger(ModelRequest.class.getName());
    private final Set<Class<? extends ModelOutput>> requestedOutputTypes = new HashSet<>();
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

    public Set<Class<? extends ModelOutput>> getOutputClasses() {
        return Collections.unmodifiableSet(requestedOutputTypes);
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

    public void requestOutputType(Class<? extends ModelOutput> outputType) {
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

    public @Nullable ModelResponse execute() {
        ModelData model = ModelData.findModelForRequest(this);
        if (model == null) {
            LOGGER.warning("No configured model supports this ModelRequest");
            return null;
        }
        return execute(model);
    }

    /**
     * Executes the request, logging and returning null on failure. Use
     * {@link #executeOrThrow(ModelData, ModelRequestListener)} to get the reason for a failure and to support
     * cancellation.
     */
    public @Nullable ModelResponse execute(ModelData model) {
        try {
            return executeOrThrow(model, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.FINE, "Interrupted while executing ModelRequest", e);
            return null;
        } catch (ModelRequestException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to execute ModelRequest", e);
            return null;
        }
    }

    /**
     * Executes the request against the given model, running any tools the model calls, until the model stops calling
     * tools. The request itself is not modified, so it can be executed again or concurrently.
     * <p>
     * The whole exchange is bounded by the client configuration's {@link ModelClientConfiguration#getTimeoutSeconds()}.
     * Interrupting the calling thread cancels the request.
     *
     * @param listener Notified of progress as the request executes, may be null
     * @throws InterruptedException if the calling thread was interrupted
     * @throws ModelRequestException if the request failed, timed out, or did not produce the requested output types
     */
    public ModelResponse executeOrThrow(ModelData model, @Nullable ModelRequestListener listener)
            throws InterruptedException, ModelRequestException {
        ModelClient<?, ?> client = model.getClient();
        if (client == null) {
            throw new ModelRequestException("No client is installed for model " + model.getModelId());
        }
        ModelRequestListener events = listener == null ? ModelRequestListener.NOOP : listener;
        ModelClientConfiguration clientConfiguration = model.getClientConfiguration();
        long timeoutSeconds = clientConfiguration == null ? 0 : clientConfiguration.getTimeoutSeconds();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);

        List<ModelInput> turnInputs = new ArrayList<>(modelInputs);
        List<ModelOutput> outputs = new ArrayList<>();
        List<ModelContent> transcript = new ArrayList<>(modelInputs);
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            List<ModelOutput> stepOutputs = takeStep(
                    client,
                    model,
                    clientConfiguration,
                    Collections.unmodifiableList(new ArrayList<>(turnInputs)),
                    events,
                    timeoutSeconds,
                    deadline);
            if (stepOutputs == null || stepOutputs.isEmpty()) {
                break;
            }
            events.onStepCompleted(stepOutputs);
            outputs.addAll(stepOutputs);
            transcript.addAll(stepOutputs);
            List<ModelInput> newInputs = generateNewInputsForOutputs(stepOutputs);
            if (newInputs.isEmpty()) {
                // The model did not call any tools, so there is nothing more to send back to it.
                break;
            }
            for (ModelInput input : newInputs) {
                if (input instanceof ToolCallResponseContent toolResult) {
                    events.onToolResult(toolResult);
                }
            }
            transcript.addAll(newInputs);
            turnInputs.addAll(newInputs);
        }
        List<String> missingOutputTypes = requestedOutputTypes.stream()
                .filter(requestedType -> outputs.stream().noneMatch(requestedType::isInstance))
                .map(Class::getSimpleName)
                .sorted()
                .toList();
        if (!missingOutputTypes.isEmpty()) {
            throw new ModelRequestException("Model " + model.getModelId()
                    + " did not produce the requested output types: " + String.join(", ", missingOutputTypes));
        }
        if (associatedConversation != null) {
            try {
                associatedConversation.addAllContent(transcript);
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING, "Failed to save conversation " + associatedConversation.getConversationId(), e);
            }
        }
        return new ModelResponse(outputs);
    }

    private List<ModelOutput> takeStep(
            ModelClient<?, ?> client,
            ModelData model,
            ModelClientConfiguration clientConfiguration,
            List<ModelInput> turnInputs,
            ModelRequestListener events,
            long timeoutSeconds,
            long deadline)
            throws InterruptedException, ModelRequestException {
        // Run the client as the caller rather than as the pool's SYSTEM identity.
        Authentication authentication = Jenkins.getAuthentication2();
        Future<List<ModelOutput>> step = Computer.threadPoolForRemoting.submit(() -> {
            try (ACLContext ignored = ACL.as2(authentication)) {
                return client.takeStep(
                        model, model.getModelConfiguration(), clientConfiguration, this, turnInputs, events);
            }
        });
        try {
            if (timeoutSeconds <= 0) {
                return step.get();
            }
            return step.get(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            step.cancel(true);
            throw e;
        } catch (TimeoutException e) {
            step.cancel(true);
            throw new ModelRequestException(
                    "Request to model " + model.getModelId() + " timed out after " + timeoutSeconds + " seconds");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new ModelRequestException(
                    "Client for model " + model.getModelId() + " failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * Used within request execution to generate all necessary inputs from anything the model outputs. Namely, executing tool calls.
     * @param stepOutputs The outputs to generate inputs for
     * @return The inputs to send back to the model, such as tool results
     */
    private static List<ModelInput> generateNewInputsForOutputs(List<ModelOutput> stepOutputs) {
        List<ModelInput> newInputs = new ArrayList<>();
        stepOutputs.forEach(modelOutput -> {
            if (modelOutput instanceof ToolCallContent toolCallOutput) {
                Tool tool = ToolRegistry.getTool(toolCallOutput.getName());
                if (tool == null) {
                    Logger.getLogger(ModelResponse.class.getName())
                            .severe("Failed to find tool model tried to call with name " + toolCallOutput.getName());
                    newInputs.add(new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null, null));
                    return;
                }
                Optional<String> toolCallResponseStr = tool.callTool(toolCallOutput.getToolArguments());
                if (toolCallResponseStr.isEmpty()) {
                    Logger.getLogger(ModelResponse.class.getName())
                            .severe("Failed to call model tool with name " + toolCallOutput.getName());
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

    @Extension
    public static class DescriptorImpl extends Descriptor<ModelRequest> {
        @Override
        public @NonNull String getDisplayName() {
            return "Model Request";
        }
    }
}
