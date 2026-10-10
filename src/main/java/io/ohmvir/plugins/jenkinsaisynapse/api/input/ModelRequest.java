package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
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
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    /**
     * The default maximum number of calls to the model while executing a request.
     */
    public static final int DEFAULT_MAX_TURNS = 25;

    private static final ExecutorService CLIENT_EXECUTOR = Executors.newCachedThreadPool(
            new NamingThreadFactory(new DaemonThreadFactory(), "ModelRequest client call"));

    private final Set<Class<? extends ModelOutput>> requestedOutputTypes = new HashSet<>();
    private final @Getter List<ModelInput> modelInputs = new ArrayList<>();

    @Setter
    private ModelConversation associatedConversation = null;

    private transient @Nullable ModelRequestListener listener;

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
        return (T) modelInputs.stream().filter(clazz::isInstance).findFirst().orElse(null);
    }

    /**
     * Replaces any inputs of the same class as the given input, for settings such as the system prompt or temperature.
     */
    public void replaceInput(ModelInput input) {
        modelInputs.removeIf(existing -> existing.getClass().equals(input.getClass()));
        modelInputs.add(input);
    }

    /**
     * Sets the listener notified of this request's progress while it executes. Clients that support streaming should
     * report response text and thinking to {@link #getListener()} as it is generated.
     */
    public void setListener(@Nullable ModelRequestListener listener) {
        this.listener = listener;
    }

    public @NonNull ModelRequestListener getListener() {
        return listener == null ? ModelRequestListener.NOOP : listener;
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
     * Executes the request, logging failures instead of throwing them.
     * Prefer {@link #executeOrThrow(ModelData, Duration, int)}, which reports why the request failed and can be cancelled.
     *
     * @return The response, or null if the request failed.
     */
    public @Nullable ModelResponse execute(ModelData model) {
        try {
            return executeOrThrow(model, null, DEFAULT_MAX_TURNS);
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
     * Executes the request against the given model. The model is called repeatedly, executing the tools it calls and
     * sending their results back, until it answers without calling a tool.
     * <p>
     * Every call to the model client is bounded by the client's configured timeout, if it has one. Interrupting the
     * calling thread cancels the request.
     * <p>
     * Tool results are appended to this request's inputs, so a request should only be executed once.
     *
     * @param model The model to execute the request against.
     * @param timeout The maximum time the whole request may take, or null for no limit beyond the client's timeout.
     * @param maxTurns The maximum number of calls to the model, or 0 or less for no limit.
     * @return The model's response.
     * @throws ModelRequestTimeoutException If the request or a call to the client took too long.
     * @throws ModelRequestException If the request could not be completed.
     * @throws InterruptedException If the calling thread was interrupted.
     */
    public @NonNull ModelResponse executeOrThrow(@NonNull ModelData model, @Nullable Duration timeout, int maxTurns)
            throws IOException, InterruptedException {
        ModelClient<?, ?> client = model.getClient();
        if (client == null) {
            throw new ModelRequestException("No installed client can execute requests for model " + model.getModelId());
        }
        ModelConfiguration configuration = model.getModelConfiguration();
        if (configuration == null) {
            throw new ModelRequestException("Model " + model.getModelId() + " is no longer configured");
        }
        ModelClientConfiguration clientConfiguration = model.getClientConfiguration();
        if (clientConfiguration == null) {
            throw new ModelRequestException("No client configuration was found for model " + model.getModelId());
        }
        Long callTimeoutSeconds = clientConfiguration.getTimeoutSeconds();
        Deadline deadline = new Deadline(timeout);
        ModelRequestListener requestListener = getListener();
        Set<String> allowedTools = getAllowedToolNames();

        List<ModelOutput> currentOutputs = new ArrayList<>();
        List<ModelContent> finalContent = new ArrayList<>();
        modelInputs.stream()
                .filter(input -> !(input instanceof InputConversationContent))
                .forEach(finalContent::add);
        for (int turn = 1; ; turn++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            List<ModelInput> turnInputs = Collections.unmodifiableList(new ArrayList<>(modelInputs));
            List<ModelOutput> stepOutputs = takeStep(
                    client, model, configuration, clientConfiguration, turnInputs, callTimeoutSeconds, deadline);
            if (stepOutputs == null || stepOutputs.isEmpty()) {
                break;
            }
            currentOutputs.addAll(stepOutputs);
            finalContent.addAll(stepOutputs);
            stepOutputs.forEach(requestListener::onOutput);
            List<ModelInput> newInputs = generateNewInputsForOutputs(stepOutputs, allowedTools, requestListener);
            if (newInputs.isEmpty()) {
                break;
            }
            finalContent.addAll(newInputs);
            modelInputs.addAll(newInputs);
            if (maxTurns > 0 && turn >= maxTurns) {
                throw new ModelRequestException("The model was still calling tools after " + maxTurns + " turns");
            }
        }
        List<String> missingOutputs = requestedOutputTypes.stream()
                .filter(requestedType -> currentOutputs.stream().noneMatch(requestedType::isInstance))
                .map(Class::getSimpleName)
                .toList();
        if (!missingOutputs.isEmpty()) {
            throw new ModelRequestException(
                    "The model's response did not contain the requested outputs: " + String.join(", ", missingOutputs));
        }
        if (associatedConversation != null) {
            associatedConversation.addAllContent(finalContent);
        }
        return new ModelResponse(currentOutputs);
    }

    private List<ModelOutput> takeStep(
            ModelClient<?, ?> client,
            ModelData model,
            ModelConfiguration configuration,
            ModelClientConfiguration clientConfiguration,
            List<ModelInput> turnInputs,
            @Nullable Long callTimeoutSeconds,
            Deadline deadline)
            throws IOException, InterruptedException {
        Authentication authentication = Jenkins.getAuthentication2();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Future<List<ModelOutput>> future = CLIENT_EXECUTOR.submit(() -> {
            Thread thread = Thread.currentThread();
            ClassLoader previousClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(contextClassLoader);
            try (ACLContext ignored = ACL.as2(authentication)) {
                return client.takeStep(model, configuration, clientConfiguration, this, turnInputs);
            } finally {
                thread.setContextClassLoader(previousClassLoader);
            }
        });
        long callTimeoutNanos = callTimeoutSeconds != null && callTimeoutSeconds > 0
                ? TimeUnit.SECONDS.toNanos(callTimeoutSeconds)
                : Long.MAX_VALUE;
        long remainingNanos = deadline.remainingNanos();
        try {
            if (remainingNanos <= 0) {
                throw new TimeoutException();
            }
            long waitNanos = Math.min(callTimeoutNanos, remainingNanos);
            return waitNanos == Long.MAX_VALUE ? future.get() : future.get(waitNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            if (remainingNanos <= callTimeoutNanos) {
                throw new ModelRequestTimeoutException(
                        "The request did not finish within " + deadline.getTimeoutSeconds() + " seconds");
            }
            throw new ModelRequestTimeoutException("Model " + model.getModelId() + " did not respond within "
                    + callTimeoutSeconds + " seconds (the client's configured timeout)");
        } catch (InterruptedException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ModelRequestException modelRequestException) {
                throw modelRequestException;
            }
            throw new ModelRequestException(
                    "Model " + model.getModelId() + " failed to respond: " + cause, cause == null ? e : cause);
        }
    }

    /**
     * @return The names of the tools the model may call: the attached tools and the tools allowed by attached skills.
     */
    public Set<String> getAllowedToolNames() {
        Set<String> names = new HashSet<>();
        for (ModelInput input : modelInputs) {
            if (input instanceof InputToolContent toolContent && toolContent.getTool() != null) {
                names.add(toolContent.getTool().getName());
            } else if (input instanceof InputSkillContent skillContent
                    && skillContent.getSkill() != null
                    && skillContent.getSkill().getAllowedTools() != null) {
                names.addAll(skillContent.getSkill().getAllowedTools());
            }
        }
        return names;
    }

    /**
     * Used within request execution to generate all necessary inputs from anything the model outputs. Namely, executing tool calls.
     * @param stepOutputs The outputs to generate inputs for
     * @param allowedTools The names of the tools the model may call
     * @param requestListener Notified of every tool result
     * @return The inputs to send back to the model
     */
    private static List<ModelInput> generateNewInputsForOutputs(
            List<ModelOutput> stepOutputs, Set<String> allowedTools, ModelRequestListener requestListener) {
        List<ModelInput> newInputs = new ArrayList<>();
        stepOutputs.forEach(modelOutput -> {
            if (modelOutput instanceof ToolCallContent toolCallOutput) {
                ToolCallResponseContent result = callTool(toolCallOutput, allowedTools);
                newInputs.add(result);
                requestListener.onToolResult(toolCallOutput, result);
            }
        });
        return newInputs;
    }

    private static ToolCallResponseContent callTool(ToolCallContent toolCallOutput, Set<String> allowedTools) {
        if (!allowedTools.contains(toolCallOutput.getName())) {
            LOGGER.warning(
                    "Model tried to call tool " + toolCallOutput.getName() + " which was not attached to the request");
            return new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null, null);
        }
        Tool tool = ToolRegistry.getTool(toolCallOutput.getName());
        if (tool == null) {
            LOGGER.severe("Failed to find tool model tried to call with name " + toolCallOutput.getName());
            return new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null, null);
        }
        Optional<String> toolCallResponseStr = tool.callTool(toolCallOutput.getToolArguments());
        if (toolCallResponseStr.isEmpty()) {
            LOGGER.severe("Failed to call model tool with name " + toolCallOutput.getName());
            return new ToolCallResponseContent(toolCallOutput.getToolUseId(), false, null, tool);
        }
        return new ToolCallResponseContent(toolCallOutput.getToolUseId(), true, toolCallResponseStr.get(), tool);
    }

    private static final class Deadline {
        private final long timeoutNanos;
        private final long startNanos = System.nanoTime();

        Deadline(@Nullable Duration timeout) {
            this.timeoutNanos =
                    timeout == null || timeout.isZero() || timeout.isNegative() ? Long.MAX_VALUE : timeout.toNanos();
        }

        long getTimeoutSeconds() {
            return TimeUnit.NANOSECONDS.toSeconds(timeoutNanos);
        }

        long remainingNanos() {
            if (timeoutNanos == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            return timeoutNanos - (System.nanoTime() - startNanos);
        }
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
