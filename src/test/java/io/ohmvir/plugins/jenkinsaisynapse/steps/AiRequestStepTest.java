package io.ohmvir.plugins.jenkinsaisynapse.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import hudson.model.Descriptor;
import hudson.model.Result;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputConversationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.MaxOutputTokensContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.SystemPromptContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelDataRetriever;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputFileContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.SnippetizerTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AiRequestStepTest {
    private static final String FULL_MODEL = "fake:full";
    private static final String TEXT_ONLY_MODEL = "fake:text-only";
    private static final String TOOL_NAME = "aiRequestTestUpper";

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        addModel("full");
        addModel("text-only");
        new UpperTool();
    }

    private static void addModel(String name) throws Descriptor.FormException {
        FakeModelConfiguration configuration = new FakeModelConfiguration(name);
        ModelsManagementLink.get().getModelConfigurations().add(configuration);
        ModelData.initializeModelDataForConfig(configuration);
    }

    private WorkflowRun run(String script, Result expected) throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(script, true));
        return j.assertBuildStatus(expected, job.scheduleBuild2(0));
    }

    @Test
    void returnsTheAnswerAsAString() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("def answer = aiRequest 'hello'\necho \"answer=${answer}\"", true));
        // Every build gets a fresh request, so nothing piles up across builds.
        for (int i = 0; i < 2; i++) {
            WorkflowRun build = j.buildAndAssertSuccess(job);
            j.assertLogContains("answer=echo: hello", build);
            j.assertLogContains("[aiRequest] Sending request to fake:", build);
        }
    }

    @Test
    void returnsTheWholeResponseAsAMap() throws Exception {
        WorkflowRun build = run("""
                def r = aiRequest(prompt: 'hello', model: '%s', systemPrompt: 'be brief', temperature: 0.5,
                    maxOutputTokens: 100, thinkingLevel: 'high', returnResponse: true)
                echo "text=${r.text}"
                echo "model=${r.model}"
                echo "finish=${r.finishReason}"
                echo "tokens=${r.usage.inputTokens}/${r.usage.outputTokens}"
                """.formatted(FULL_MODEL), Result.SUCCESS);
        j.assertLogContains(
                "text=echo: hello [system=be brief] [temperature=0.5] [maxOutputTokens=100] [thinking=HIGH]", build);
        j.assertLogContains("model=" + FULL_MODEL, build);
        j.assertLogContains("finish=STOP", build);
        j.assertLogContains("tokens=10/5", build);
        j.assertLogContains("[aiRequest] " + FULL_MODEL + " finished (stop, 10 input tokens, 5 output tokens)", build);
    }

    @Test
    void callsToolsUntilTheModelAnswers() throws Exception {
        WorkflowRun build = run("""
                def r = aiRequest prompt: 'use tool', tools: ['%s'], returnResponse: true
                echo "text=${r.text}"
                echo "allText=${r.allText}"
                def call = r.toolCalls[0]
                echo "call=${call.name} ${call.arguments.text} ${call.successful} ${call.result}"
                """.formatted(TOOL_NAME), Result.SUCCESS);
        j.assertLogContains("[aiRequest] Calling tool " + TOOL_NAME, build);
        j.assertLogContains("text=tool said HELLO", build);
        j.assertLogContains("allText=Let me check. tool said HELLO", build);
        j.assertLogContains("call=" + TOOL_NAME + " hello true HELLO", build);
    }

    @Test
    void refusesToolsThatWereNotAttached() throws Exception {
        WorkflowRun build = run("""
                def r = aiRequest prompt: 'use tool', model: '%s', returnResponse: true
                echo "successful=${r.toolCalls[0].successful}"
                """.formatted(FULL_MODEL), Result.SUCCESS);
        j.assertLogContains("[aiRequest] Tool " + TOOL_NAME + " failed", build);
        j.assertLogContains("successful=false", build);
    }

    @Test
    void startsFromAStoredPrompt() throws Exception {
        storePrompt(new SimplePrompt("base", "Job is ${JOB_NAME}"));
        WorkflowRun build = run("""
                echo "plain=${aiRequest(promptId: 'base', prompt: 'extra', model: '%1$s')}"
                echo "expanded=${aiRequest(promptId: 'base', model: '%1$s', expandEnvVars: true)}"
                """.formatted(FULL_MODEL), Result.SUCCESS);
        j.assertLogContains("plain=echo: Job is ${JOB_NAME} | extra", build);
        j.assertLogContains("expanded=echo: Job is " + build.getParent().getFullName(), build);
    }

    @Test
    void attachesWorkspaceFilesAndSavesProducedFiles() throws Exception {
        WorkflowRun build = run("""
                node {
                    writeFile file: 'notes.txt', text: 'file body'
                    echo "answer=${aiRequest(prompt: 'read', model: '%1$s', files: ['notes.txt'])}"
                    def r = aiRequest prompt: 'make file', model: '%1$s', outputDir: 'out', returnResponse: true
                    echo "path=${r.files[0].path}"
                    echo "content=${readFile(r.files[0].path)}"
                }
                """.formatted(FULL_MODEL), Result.SUCCESS);
        j.assertLogContains("answer=echo: read [file notes.txt text/plain: file body]", build);
        j.assertLogContains("path=out/evil.txt", build);
        j.assertLogContains("content=generated", build);
    }

    @Test
    void continuesConversationsAcrossBuilds() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(
                "echo \"answer=${aiRequest(prompt: 'hi', model: '" + FULL_MODEL + "', conversationId: 'conv')}\"",
                true));
        j.assertLogContains("answer=echo: hi [history=0]", j.buildAndAssertSuccess(job));
        j.assertLogNotContains("[history=0]", j.buildAndAssertSuccess(job));
    }

    @Test
    void printsStreamedTextOnce() throws Exception {
        WorkflowRun build = run("aiRequest prompt: 'stream', model: '" + FULL_MODEL + "'", Result.SUCCESS);
        assertEquals(1, countOccurrences(JenkinsRule.getLog(build), "streamed text"));
    }

    @Test
    void failsWithoutAPrompt() throws Exception {
        j.assertLogContains(
                "aiRequest needs a prompt or a promptId", run("aiRequest model: '" + FULL_MODEL + "'", Result.FAILURE));
    }

    @Test
    void failsForAnUnknownModel() throws Exception {
        WorkflowRun build = run("aiRequest prompt: 'hi', model: 'nope'", Result.FAILURE);
        j.assertLogContains("Unknown model 'nope'", build);
        j.assertLogContains(FULL_MODEL, build);
    }

    @Test
    void failsWhenTheModelDoesNotSupportTheRequest() throws Exception {
        WorkflowRun build = run(
                "aiRequest prompt: 'hi', model: '" + TEXT_ONLY_MODEL + "', tools: ['" + TOOL_NAME + "']",
                Result.FAILURE);
        j.assertLogContains("Model " + TEXT_ONLY_MODEL + " does not support capability TOOLS", build);
    }

    @Test
    void failsForAnUnknownTool() throws Exception {
        WorkflowRun build = run("aiRequest prompt: 'hi', tools: ['nope']", Result.FAILURE);
        j.assertLogContains("Unknown tool 'nope'", build);
    }

    @Test
    void failsWhenTheTimeoutIsReached() throws Exception {
        WorkflowRun build = run("aiRequest prompt: 'sleep', model: '" + FULL_MODEL + "', timeout: 1", Result.FAILURE);
        j.assertLogContains("The request did not finish within 1 seconds", build);
    }

    @Test
    void canBeAborted() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("aiRequest prompt: 'sleep', model: '" + FULL_MODEL + "'", true));
        WorkflowRun build = job.scheduleBuild2(0).waitForStart();
        j.waitForMessage("[aiRequest] Sending request to " + FULL_MODEL, build);
        build.doStop();
        j.assertBuildStatus(Result.ABORTED, j.waitForCompletion(build));
    }

    @Test
    void roundTripsThroughTheSnippetGenerator() throws Exception {
        SnippetizerTester tester = new SnippetizerTester(j);
        AiRequestStep step = new AiRequestStep("hello");
        tester.assertRoundTrip(step, "aiRequest 'hello'");
        step.setModel(FULL_MODEL);
        step.setTemperature(0.5);
        step.setTools(List.of(TOOL_NAME));
        step.setReturnResponse(true);
        tester.assertRoundTrip(
                step,
                "aiRequest model: '" + FULL_MODEL
                        + "', prompt: 'hello', returnResponse: true, temperature: 0.5, tools: ['" + TOOL_NAME + "']");
    }

    @SuppressWarnings("unchecked")
    private static void storePrompt(PromptConfiguration prompt) throws Exception {
        Field prompts = BasePromptsManagementLink.class.getDeclaredField("prompts");
        prompts.setAccessible(true);
        ((Map<String, PromptConfiguration>) prompts.get(BasePromptsManagementLink.get()))
                .put(prompt.getPromptId(), prompt);
    }

    private static int countOccurrences(String text, String part) {
        int count = 0;
        for (int index = text.indexOf(part); index >= 0; index = text.indexOf(part, index + 1)) {
            count++;
        }
        return count;
    }

    public static class FakeModelConfiguration extends ModelConfiguration {
        public FakeModelConfiguration(String modelName) throws Descriptor.FormException {
            super(modelName, modelName);
        }

        @Override
        public String getProviderType() {
            return "fake";
        }
    }

    @TestExtension
    public static class FakeClientConfiguration extends ModelClientConfiguration {
        public FakeClientConfiguration() throws Descriptor.FormException {
            super(0L);
        }
    }

    @TestExtension
    public static class FakeModelDataRetriever extends ModelDataRetriever<FakeModelConfiguration> {
        public FakeModelDataRetriever() {
            super(FakeModelConfiguration.class);
        }

        @Override
        public ModelData retrieveFromConfiguration(FakeModelConfiguration configuration) {
            ModelData data = new ModelData(configuration);
            if (configuration.getModelName().equals("full")) {
                data.setCapabilities(new ArrayList<>(Arrays.asList(ModelCapability.values())));
                data.setInputs(new ArrayList<>(Arrays.asList(ModelInputType.values())));
                data.setOutputs(new ArrayList<>(Arrays.asList(ModelOutputType.values())));
                data.setSupportedThinkingLevels(new ArrayList<>(Arrays.asList(ModelThinkingLevel.values())));
            } else {
                data.setInputs(new ArrayList<>(List.of(ModelInputType.TEXT)));
                data.setOutputs(new ArrayList<>(List.of(ModelOutputType.UNSTRUCTURED_TEXT)));
            }
            return data;
        }
    }

    /**
     * Answers by echoing the request, or behaves as the prompt says: calls a tool, streams, sleeps or makes a file.
     */
    @TestExtension
    public static class FakeClient extends ModelClient<FakeModelConfiguration, FakeClientConfiguration> {
        @Override
        protected List<ModelOutput> takeStepImpl(
                ModelData modelData,
                FakeModelConfiguration configuration,
                FakeClientConfiguration clientConfiguration,
                ModelRequest request,
                List<ModelInput> turnInputs) {
            List<String> texts = turnInputs.stream()
                    .filter(InputTextContent.class::isInstance)
                    .map(input -> ((InputTextContent) input).getText())
                    .toList();
            String prompt = texts.isEmpty() ? "" : texts.get(texts.size() - 1);
            ToolCallResponseContent toolResult = turnInputs.stream()
                    .filter(ToolCallResponseContent.class::isInstance)
                    .map(ToolCallResponseContent.class::cast)
                    .reduce((first, second) -> second)
                    .orElse(null);
            if (toolResult != null) {
                return answer("tool said " + toolResult.getResponseContent());
            }
            switch (prompt) {
                case "use tool" -> {
                    JsonObject arguments = new JsonObject();
                    arguments.addProperty("text", "hello");
                    return List.of(
                            new OutputTextContent("Let me check. "),
                            new ToolCallContent("call-1", arguments, TOOL_NAME),
                            new FinishReasonContent(ModelFinishReason.TOOL_CALLS));
                }
                case "stream" -> {
                    request.getListener().onTextDelta("streamed ");
                    request.getListener().onTextDelta("text");
                    return answer("streamed text");
                }
                case "sleep" -> {
                    try {
                        Thread.sleep(60_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return List.of();
                }
                case "make file" -> {
                    return List.of(
                            new OutputFileContent(
                                    "../evil.txt", "generated".getBytes(StandardCharsets.UTF_8), "text/plain"),
                            new FinishReasonContent(ModelFinishReason.STOP));
                }
                default -> {
                    return answer(describe(texts, turnInputs));
                }
            }
        }

        private static List<ModelOutput> answer(String text) {
            return List.of(
                    new OutputTextContent(text),
                    new FinishReasonContent(ModelFinishReason.STOP),
                    new TokenUtilizationContent(10, 5, 0));
        }

        private static String describe(List<String> texts, List<ModelInput> inputs) {
            StringBuilder description = new StringBuilder("echo: ").append(String.join(" | ", texts));
            for (ModelInput input : inputs) {
                switch (input) {
                    case SystemPromptContent systemPrompt ->
                        description
                                .append(" [system=")
                                .append(systemPrompt.getSystemPrompt())
                                .append(']');
                    case TemperatureContent temperature ->
                        description
                                .append(" [temperature=")
                                .append(temperature.getTemperature())
                                .append(']');
                    case MaxOutputTokensContent maxOutputTokens ->
                        description
                                .append(" [maxOutputTokens=")
                                .append(maxOutputTokens.getMaxOutputTokens())
                                .append(']');
                    case ThinkingLevelContent thinkingLevel ->
                        description
                                .append(" [thinking=")
                                .append(thinkingLevel.getThinkingLevel())
                                .append(']');
                    case InputFileContent file ->
                        description
                                .append(" [file ")
                                .append(file.getFileId())
                                .append(' ')
                                .append(file.getContentType())
                                .append(": ")
                                .append(new String(file.getFileData(), StandardCharsets.UTF_8))
                                .append(']');
                    case InputConversationContent conversation ->
                        description
                                .append(" [history=")
                                .append(conversation
                                        .getConversation()
                                        .getConversation()
                                        .size())
                                .append(']');
                    default -> {}
                }
            }
            return description.toString();
        }
    }

    public static class UpperTool extends Tool {
        public UpperTool() throws NoSuchMethodException {}

        @Override
        public String getName() {
            return TOOL_NAME;
        }

        @Override
        public String getDescription() {
            return "Upper-cases text";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of(new ToolArgumentDescription("text", String.class, "The text to upper-case", true));
        }

        public String aiRequestTestUpper(String text) {
            return text.toUpperCase(Locale.ROOT);
        }
    }
}
