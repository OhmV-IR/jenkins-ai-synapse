package io.ohmvir.plugins.jenkinsaisynapse.api.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelContext;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.MaxStepsExceededException;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.MissingOutputException;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.ModelInvocationException;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.ModelTimeoutException;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Role;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.TextPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ToolSpec;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.StepResult;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.TokenUsage;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AgentLoopTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ToolSpec ECHO_TOOL = new ToolSpec("echo", "Echoes its text", MAPPER.createObjectNode());

    private TestModelConfiguration configuration;
    private TestClientConfiguration clientConfiguration;
    private ModelData model;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        configuration = new TestModelConfiguration();
        clientConfiguration = new TestClientConfiguration(0L);
        model = new ModelData(configuration);
    }

    private ModelResponse run(AgentLoop loop, ScriptedClient client, ModelRequest request) throws Exception {
        return loop.run(client, configuration, clientConfiguration, model, request);
    }

    private static ModelRequest request(String prompt) {
        return ModelRequest.builder().user(prompt).tool(ECHO_TOOL).build();
    }

    private static ToolCallPart echoCall(String id) {
        ObjectNode arguments = MAPPER.createObjectNode();
        arguments.put("text", "hi");
        return new ToolCallPart(id, "echo", arguments);
    }

    private static final ToolExecutor UPPER_CASE_EXECUTOR = (request, call) -> ToolResultPart.success(
            call, call.getArguments().get("text").asText().toUpperCase(Locale.ROOT));

    @Test
    void stopsWhenTheModelAnswers() throws Exception {
        ScriptedClient client = new ScriptedClient(
                transcript -> StepResult.of(List.of(new TextPart("hello")), null, new TokenUsage(3, 2, 0)));
        ModelResponse response = run(AgentLoop.builder().build(), client, request("hi"));

        assertEquals("hello", response.getText());
        assertEquals(ModelFinishReason.STOP, response.getFinishReason());
        assertEquals(1, response.getSteps());
        assertEquals(1, client.transcripts.size());
        assertEquals(new TokenUsage(3, 2, 0), response.getUsage());
        assertEquals(List.of(Role.USER, Role.ASSISTANT), roles(response.getTranscript()));
    }

    @Test
    void sendsEachToolResultRightAfterTheCallItAnswers() throws Exception {
        ScriptedClient client = new ScriptedClient(transcript -> {
            Message last = transcript.get(transcript.size() - 1);
            if (last.getRole() == Role.TOOL) {
                return StepResult.of(
                        List.of(new TextPart("done: "
                                + last.getParts(ToolResultPart.class).get(0).getContent())),
                        ModelFinishReason.STOP,
                        new TokenUsage(5, 1, 0));
            }
            return StepResult.of(List.of(new TextPart("checking "), echoCall("call-1")), null, new TokenUsage(4, 2, 1));
        });
        ModelResponse response =
                run(AgentLoop.builder().toolExecutor(UPPER_CASE_EXECUTOR).build(), client, request("hi"));

        assertEquals("done: HI", response.getText());
        assertEquals("checking done: HI", response.getAllText());
        assertEquals(2, response.getSteps());
        assertEquals(new TokenUsage(9, 3, 1), response.getUsage());
        List<Message> secondCall = client.transcripts.get(1);
        assertEquals(List.of(Role.USER, Role.ASSISTANT, Role.TOOL), roles(secondCall));
        assertEquals(
                secondCall.get(1).getToolCalls().get(0).getId(),
                secondCall.get(2).getParts(ToolResultPart.class).get(0).getToolCallId());
        assertEquals(List.of(Role.ASSISTANT, Role.TOOL, Role.ASSISTANT), roles(response.getNewMessages()));
    }

    @Test
    void leavesTheRequestUnchangedSoItCanBeRunAgain() throws Exception {
        ScriptedClient client = new ScriptedClient(transcript -> transcript.size() == 1
                ? StepResult.of(List.of(echoCall("call-1")), null, null)
                : StepResult.of(List.of(new TextPart("done")), null, null));
        AgentLoop loop = AgentLoop.builder().toolExecutor(UPPER_CASE_EXECUTOR).build();
        ModelRequest request = request("hi");

        ModelResponse first = run(loop, client, request);
        ModelResponse second = run(loop, client, request);

        assertEquals(1, request.getMessages().size());
        assertEquals(roles(first.getTranscript()), roles(second.getTranscript()));
        // Each run's first call sees only the request's own message.
        assertEquals(1, client.transcripts.get(0).size());
        assertEquals(1, client.transcripts.get(2).size());
    }

    @Test
    void derivedRequestsDoNotChangeTheirBase() {
        ModelRequest base =
                ModelRequest.builder().systemPrompt("base").user("one").build();
        ModelRequest derived =
                base.toBuilder().addUserParts(new TextPart("two")).user("three").build();

        assertEquals(1, base.getMessages().size());
        assertEquals("one", base.getMessages().get(0).getText());
        assertEquals(2, derived.getMessages().size());
        assertEquals("onetwo", derived.getMessages().get(0).getText());
        assertEquals("base", derived.getSystemPrompt());
    }

    @Test
    void failsWhenTheModelKeepsCallingTools() {
        ScriptedClient client = new ScriptedClient(
                transcript -> StepResult.of(List.of(echoCall("call-" + transcript.size())), null, null));
        AgentLoop loop = AgentLoop.builder()
                .maxSteps(3)
                .toolExecutor(UPPER_CASE_EXECUTOR)
                .build();

        MaxStepsExceededException e =
                assertThrows(MaxStepsExceededException.class, () -> run(loop, client, request("hi")));
        assertEquals(3, client.transcripts.size());
        assertEquals(7, e.getTranscript().size());
    }

    @Test
    void stopsWhenTheModelReportsToolCallsWithoutMakingAny() throws Exception {
        ScriptedClient client = new ScriptedClient(
                transcript -> StepResult.of(List.of(new TextPart("hmm")), ModelFinishReason.TOOL_CALLS, null));
        ModelResponse response = run(AgentLoop.builder().build(), client, request("hi"));

        assertEquals(1, client.transcripts.size());
        assertEquals("hmm", response.getText());
    }

    @Test
    void refusesToolsTheRequestDoesNotOffer() throws Exception {
        ScriptedClient client = new ScriptedClient(transcript -> transcript.size() == 1
                ? StepResult.of(List.of(new ToolCallPart("call-1", "rm-rf", MAPPER.createObjectNode())), null, null)
                : StepResult.of(List.of(new TextPart("ok")), null, null));
        ModelResponse response = run(AgentLoop.builder().build(), client, request("hi"));

        ToolResultPart result = response.getToolResults().get(0);
        assertTrue(result.isError());
        assertTrue(result.getContent().contains("not available"), result.getContent());
    }

    @Test
    void reportsClientFailuresWithTheirCause() {
        IOException failure = new IOException("connection refused");
        ScriptedClient unreachable = new ScriptedClient(transcript -> {
            throw failure;
        });
        ModelInvocationException e = assertThrows(
                ModelInvocationException.class, () -> run(AgentLoop.builder().build(), unreachable, request("hi")));
        assertSame(failure, e.getCause());
        assertTrue(e.getMessage().contains("could not be reached"), e.getMessage());

        ModelInvocationException providerError = new ModelInvocationException("401 Unauthorized: invalid API key");
        ScriptedClient unauthorized = new ScriptedClient(transcript -> {
            throw providerError;
        });
        assertSame(
                providerError,
                assertThrows(
                        ModelInvocationException.class,
                        () -> run(AgentLoop.builder().build(), unauthorized, request("hi"))));
    }

    @Test
    void enforcesTheRequestTimeoutAndTheClientTimeout() throws Exception {
        ScriptedClient sleeping = new ScriptedClient(transcript -> {
            Thread.sleep(60_000);
            return StepResult.of(List.of(new TextPart("late")), null, null);
        });
        ModelTimeoutException requestTimeout = assertThrows(
                ModelTimeoutException.class,
                () -> run(AgentLoop.builder().timeout(Duration.ofSeconds(1)).build(), sleeping, request("hi")));
        assertTrue(requestTimeout.getMessage().contains("did not finish within 1 seconds"));

        clientConfiguration = new TestClientConfiguration(1L);
        ModelTimeoutException clientTimeout = assertThrows(
                ModelTimeoutException.class, () -> run(AgentLoop.builder().build(), sleeping, request("hi")));
        assertTrue(clientTimeout.getMessage().contains("configured timeout"));
    }

    @Test
    void failsWhenARequiredOutputIsMissing() {
        ScriptedClient client =
                new ScriptedClient(transcript -> StepResult.of(List.of(new TextPart("no image")), null, null));
        ModelRequest request = ModelRequest.builder()
                .user("draw")
                .requireOutput(ModelOutputType.IMAGE)
                .build();

        MissingOutputException e = assertThrows(
                MissingOutputException.class, () -> run(AgentLoop.builder().build(), client, request));
        assertEquals(Set.of(ModelOutputType.IMAGE), e.getMissingOutputs());
        assertEquals("no image", e.getResponse().getText());
    }

    @Test
    void canBeInterrupted() {
        ScriptedClient client =
                new ScriptedClient(transcript -> StepResult.of(List.of(new TextPart("hi")), null, null));
        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () -> run(AgentLoop.builder().build(), client, request("hi")));
        assertFalse(Thread.interrupted());
    }

    @Test
    void passesTheRequestAndListenerToTheClient() throws Exception {
        AtomicReference<String> streamed = new AtomicReference<>("");
        AgentListener listener = new AgentListener() {
            @Override
            public void onTextDelta(String delta) {
                streamed.updateAndGet(text -> text + delta);
            }
        };
        ScriptedClient client = new ScriptedClient((context, transcript) -> {
            context.getListener().onTextDelta(context.getRequest().getSystemPrompt());
            return StepResult.of(List.of(new TextPart("ok")), null, null);
        });
        ModelRequest request =
                ModelRequest.builder().systemPrompt("be brief").user("hi").build();
        run(AgentLoop.builder().listener(listener).build(), client, request);

        assertEquals("be brief", streamed.get());
    }

    @Test
    void validatesMessages() {
        ToolResultPart result = new ToolResultPart("call-1", "echo", "x", false);
        assertThrows(IllegalArgumentException.class, () -> Message.user(result));
        assertThrows(IllegalArgumentException.class, () -> Message.user(echoCall("call-1")));
        assertThrows(IllegalStateException.class, () -> ModelRequest.builder().build());
        assertEquals(
                ModelFinishReason.TOOL_CALLS,
                StepResult.of(List.of(echoCall("call-1")), null, null).finishReason());
    }

    private static List<Role> roles(List<Message> messages) {
        return messages.stream().map(Message::getRole).toList();
    }

    @FunctionalInterface
    interface Script {
        StepResult step(ModelContext<TestModelConfiguration, TestClientConfiguration> context, List<Message> transcript)
                throws Exception;
    }

    @FunctionalInterface
    interface TranscriptScript {
        StepResult step(List<Message> transcript) throws Exception;
    }

    /**
     * A client whose answers are scripted from the transcript it receives. It records every transcript.
     */
    static class ScriptedClient extends ModelClient<TestModelConfiguration, TestClientConfiguration> {
        final List<List<Message>> transcripts = new ArrayList<>();
        private final Script script;

        ScriptedClient(Script script) {
            this.script = script;
        }

        ScriptedClient(TranscriptScript script) {
            this((context, transcript) -> script.step(transcript));
        }

        @Override
        public StepResult step(
                ModelContext<TestModelConfiguration, TestClientConfiguration> context, List<Message> transcript)
                throws ModelInvocationException, IOException, InterruptedException {
            synchronized (transcripts) {
                transcripts.add(transcript);
            }
            try {
                return script.step(context, transcript);
            } catch (ModelInvocationException | IOException | InterruptedException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static class TestModelConfiguration extends ModelConfiguration {
        TestModelConfiguration() throws Descriptor.FormException {
            super("test-model", "Test model");
        }

        @Override
        public String getProviderType() {
            return "test";
        }
    }

    static class TestClientConfiguration extends ModelClientConfiguration {
        TestClientConfiguration(Long timeoutSeconds) throws Descriptor.FormException {
            super(timeoutSeconds);
        }
    }
}
