package io.ohmvir.plugins.jenkinsaisynapse.api.agent;

import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelContext;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.MaxStepsExceededException;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.MissingOutputException;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.ModelException;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.ModelInvocationException;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.ModelTimeoutException;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.StepResult;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.TokenUsage;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jenkins.model.Jenkins;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Runs a {@link ModelRequest}: calls the model, and while it asks for tools, runs them and calls it again with their
 * results, until it finishes for any other reason or the maximum number of steps is reached.
 * <p>
 * The loop owns the transcript: each step's assistant message is added to it, followed by a tool message answering
 * every tool call in it, so the stateless client always receives the whole conversation. The request itself is never
 * changed. Interrupting the calling thread cancels the run.
 * <pre>
 * ModelResponse response = AgentLoop.builder().maxSteps(10).timeout(Duration.ofMinutes(5)).build()
 *         .run(ModelData.findModelForRequest(request), request);
 * </pre>
 */
public final class AgentLoop {
    public static final int DEFAULT_MAX_STEPS = 25;

    private static final ExecutorService CLIENT_EXECUTOR =
            Executors.newCachedThreadPool(new NamingThreadFactory(new DaemonThreadFactory(), "AgentLoop model call"));

    private final int maxSteps;
    private final @Nullable Duration timeout;
    private final AgentListener listener;
    private final ToolExecutor toolExecutor;

    private AgentLoop(Builder builder) {
        this.maxSteps = builder.maxSteps;
        this.timeout = builder.timeout;
        this.listener = builder.listener;
        this.toolExecutor = builder.toolExecutor;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs the request on the given model, using the client installed for it.
     *
     * @throws ModelInvocationException If no client is installed for the model, or the client failed.
     * @throws ModelTimeoutException If a call to the model or the whole run took too long.
     * @throws MaxStepsExceededException If the model was still calling tools after the maximum number of steps.
     * @throws MissingOutputException If the response lacks an output the request requires.
     * @throws InterruptedException If the calling thread was interrupted.
     */
    public ModelResponse run(ModelData model, ModelRequest request) throws ModelException, InterruptedException {
        ModelClient<?, ?> client = model.getClient();
        if (client == null) {
            throw new ModelInvocationException("No installed client can call model " + model.getModelId());
        }
        ModelConfiguration configuration = model.getModelConfiguration();
        if (configuration == null) {
            throw new ModelInvocationException("Model " + model.getModelId() + " is no longer configured");
        }
        ModelClientConfiguration clientConfiguration = model.getClientConfiguration();
        if (clientConfiguration == null) {
            throw new ModelInvocationException("No client configuration was found for model " + model.getModelId());
        }
        return run(client, configuration, clientConfiguration, model, request);
    }

    /**
     * Runs the request with the given client and configuration, without looking them up.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ModelResponse run(
            ModelClient<?, ?> client,
            ModelConfiguration configuration,
            ModelClientConfiguration clientConfiguration,
            ModelData model,
            ModelRequest request)
            throws ModelException, InterruptedException {
        ModelContext context = new ModelContext(model, configuration, clientConfiguration, request, listener);
        Long callTimeoutSeconds = clientConfiguration.getTimeoutSeconds();
        long startNanos = System.nanoTime();

        List<Message> transcript = new ArrayList<>(request.getMessages());
        int newMessagesStart = transcript.size();
        TokenUsage usage = null;
        for (int step = 1; ; step++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            StepResult result = callModel(
                    (ModelClient) client, context, List.copyOf(transcript), model, callTimeoutSeconds, startNanos);
            transcript.add(result.message());
            if (result.usage() != null) {
                usage = usage == null ? result.usage() : usage.plus(result.usage());
            }
            listener.onStep(step, result);

            List<ToolCallPart> toolCalls = result.message().getToolCalls();
            if (result.finishReason() != ModelFinishReason.TOOL_CALLS || toolCalls.isEmpty()) {
                ModelResponse response = new ModelResponse(
                        model.getModelId(), transcript, newMessagesStart, result.finishReason(), usage, step);
                checkRequiredOutputs(request, response);
                return response;
            }
            List<ToolResultPart> toolResults = new ArrayList<>();
            for (ToolCallPart call : toolCalls) {
                ToolResultPart toolResult = toolExecutor.execute(request, call);
                toolResults.add(toolResult);
                listener.onToolResult(call, toolResult);
            }
            transcript.add(Message.toolResults(toolResults));
            if (maxSteps > 0 && step >= maxSteps) {
                throw new MaxStepsExceededException(maxSteps, transcript);
            }
        }
    }

    private StepResult callModel(
            ModelClient<ModelConfiguration, ModelClientConfiguration> client,
            ModelContext<ModelConfiguration, ModelClientConfiguration> context,
            List<Message> transcript,
            ModelData model,
            @Nullable Long callTimeoutSeconds,
            long startNanos)
            throws ModelException, InterruptedException {
        Authentication authentication = Jenkins.getAuthentication2();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Future<StepResult> future = CLIENT_EXECUTOR.submit(() -> {
            Thread thread = Thread.currentThread();
            ClassLoader previousClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(contextClassLoader);
            try (ACLContext ignored = ACL.as2(authentication)) {
                return client.step(context, transcript);
            } finally {
                thread.setContextClassLoader(previousClassLoader);
            }
        });
        long callTimeoutNanos = callTimeoutSeconds != null && callTimeoutSeconds > 0
                ? TimeUnit.SECONDS.toNanos(callTimeoutSeconds)
                : Long.MAX_VALUE;
        long remainingNanos = timeout == null ? Long.MAX_VALUE : timeout.toNanos() - (System.nanoTime() - startNanos);
        try {
            if (remainingNanos <= 0) {
                throw new TimeoutException();
            }
            long waitNanos = Math.min(callTimeoutNanos, remainingNanos);
            StepResult result =
                    waitNanos == Long.MAX_VALUE ? future.get() : future.get(waitNanos, TimeUnit.NANOSECONDS);
            if (result == null) {
                throw new ModelInvocationException("The client for model " + model.getModelId() + " returned nothing");
            }
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            if (remainingNanos <= callTimeoutNanos && timeout != null) {
                throw new ModelTimeoutException(
                        "The request did not finish within " + timeout.toSeconds() + " seconds");
            }
            throw new ModelTimeoutException("Model " + model.getModelId() + " did not respond within "
                    + callTimeoutSeconds + " seconds (the client's configured timeout)");
        } catch (InterruptedException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ModelInvocationException invocationException) {
                throw invocationException;
            }
            if (cause instanceof InterruptedException interruptedException) {
                throw interruptedException;
            }
            String detail = cause instanceof IOException ? "could not be reached" : "failed";
            throw new ModelInvocationException(
                    "Model " + model.getModelId() + " " + detail + ": " + cause, cause == null ? e : cause);
        }
    }

    private static void checkRequiredOutputs(ModelRequest request, ModelResponse response)
            throws MissingOutputException {
        if (request.getRequiredOutputs().isEmpty()) {
            return;
        }
        Set<ModelOutputType> missing = EnumSet.copyOf(request.getRequiredOutputs());
        missing.removeAll(response.getOutputTypes());
        if (!missing.isEmpty()) {
            throw new MissingOutputException(missing, response);
        }
    }

    public static final class Builder {
        private int maxSteps = DEFAULT_MAX_STEPS;
        private @Nullable Duration timeout;
        private AgentListener listener = AgentListener.NOOP;
        private ToolExecutor toolExecutor = new RegistryToolExecutor();

        private Builder() {}

        /**
         * @param maxSteps The maximum number of calls to the model, or 0 for no limit.
         */
        public Builder maxSteps(int maxSteps) {
            if (maxSteps < 0) {
                throw new IllegalArgumentException("maxSteps must not be negative");
            }
            this.maxSteps = maxSteps;
            return this;
        }

        /**
         * @param timeout The maximum time the whole run may take, or null for no limit beyond the client's timeout.
         */
        public Builder timeout(@Nullable Duration timeout) {
            this.timeout = timeout == null || timeout.isZero() || timeout.isNegative() ? null : timeout;
            return this;
        }

        public Builder listener(AgentListener listener) {
            this.listener = Objects.requireNonNull(listener, "listener");
            return this;
        }

        /**
         * @param toolExecutor Runs the tools the model calls. Defaults to {@link RegistryToolExecutor}.
         */
        public Builder toolExecutor(ToolExecutor toolExecutor) {
            this.toolExecutor = Objects.requireNonNull(toolExecutor, "toolExecutor");
            return this;
        }

        public AgentLoop build() {
            return new AgentLoop(this);
        }
    }
}
