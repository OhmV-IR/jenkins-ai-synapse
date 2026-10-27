package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ConversationEntry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversationsAction;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestListener;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.StopSequencesContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopKContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopPContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelProvider;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.steps.AiRequestStep;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.DataBoundConstructor;

@WithJenkins
public class AiRequestStepTest {
    private JenkinsRule j;

    @BeforeEach
    public void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        ModelsConfiguration.get().setModels(List.of(new FakeModelConfiguration("echo")));
    }

    @Test
    public void pipelineStepReturnsTextOrDetails() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        String script = """
                def answer = aiRequest 'hello'
                echo "answer=${answer}"
                def details = aiRequest(prompt: 'hello', model: 'fake:echo', returnDetails: true)
                echo "text=${details.text} tokens=${details.usage.outputTokens} model=${details.model}"
                """;
        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun b = j.buildAndAssertSuccess(p);
        j.assertLogContains("answer=echo: hello", b);
        j.assertLogContains("text=echo: hello tokens=5 model=fake:echo", b);
        j.assertLogContains("[AI] Sending request to fake:echo", b);

        ModelConversationsAction action = b.getAction(ModelConversationsAction.class);
        assertNotNull(action);
        assertEquals(2, action.getConversations().size());
        ModelConversation conversation = action.getConversations().get(0);
        assertEquals("fake:echo", conversation.getModelId());
        assertTrue(conversation.getEntries().stream()
                .anyMatch(
                        entry -> "OutputTextContent".equals(entry.getType()) && "echo: hello".equals(entry.getText())));

        // The conversations are stored with the build as plain entries, and shown on it.
        String buildXml = Files.readString(new File(b.getRootDir(), "build.xml").toPath());
        assertTrue(buildXml.contains("<type>OutputTextContent</type>"), buildXml);
        assertFalse(buildXml.contains("<dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent>"), buildXml);
        ModelConversationsAction copy = (ModelConversationsAction) Run.XSTREAM2.fromXML(Run.XSTREAM2.toXML(action));
        assertEquals(2, copy.getConversations().size());
        assertEquals(
                conversation.getEntries().size(),
                copy.getConversations().get(0).getEntries().size());
        assertTrue(copy.getConversations().get(0).getConversation().isEmpty());
        j.createWebClient().getPage(b, "ai-conversations");
    }

    @Test
    public void pipelineStepRunsTools() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        String script = """
                def answer = aiRequest(prompt: 'hi there', tools: ['shout'])
                echo "answer=${answer}"
                """;
        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun b = j.buildAndAssertSuccess(p);
        j.assertLogContains("[AI] Calling tool shout", b);
        j.assertLogContains("[AI] Tool shout returned: HI THERE", b);
        j.assertLogContains("answer=tool said HI THERE", b);
    }

    @Test
    public void pipelineStepReportsUnknownModelsAndTools() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("aiRequest(prompt: 'hi', model: 'missing:model')", true));
        j.assertLogContains("Unknown model 'missing:model'", j.buildAndAssertStatus(Result.FAILURE, p));

        p.setDefinition(new CpsFlowDefinition("aiRequest(prompt: 'hi', tools: ['missing'])", true));
        j.assertLogContains("Unknown tool 'missing'", j.buildAndAssertStatus(Result.FAILURE, p));

        p.setDefinition(new CpsFlowDefinition("aiRequest(promptId: 'missing')", true));
        j.assertLogContains("No base prompt with id 'missing'", j.buildAndAssertStatus(Result.FAILURE, p));
    }

    @Test
    public void abortingTheBuildCancelsTheRequest() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("aiRequest 'sleep'", true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        j.waitForMessage("[AI] Sending request", b);
        b.doStop();
        j.assertBuildStatus(Result.ABORTED, j.waitForCompletion(b));
    }

    @Test
    public void requestsTimeOut() throws Exception {
        ExtensionList.lookupSingleton(FakeClientConfiguration.class).setTimeoutSeconds(1);
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("aiRequest 'sleep'", true));
        j.assertLogContains("timed out after 1 seconds", j.buildAndAssertStatus(Result.FAILURE, p));
    }

    @Test
    public void timeoutMustNotBeNegative() {
        FakeClientConfiguration configuration = ExtensionList.lookupSingleton(FakeClientConfiguration.class);
        assertEquals(ModelClientConfiguration.DEFAULT_TIMEOUT_SECONDS, configuration.getTimeoutSeconds());
        assertThrows(IllegalArgumentException.class, () -> configuration.setTimeoutSeconds(-1));
    }

    @Test
    public void freestyleStepSurvivesConfigRoundtrip() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        ModelRequestExecutionStep step = new ModelRequestExecutionStep();
        step.setPrompt("Hello ${WHO}");
        step.setSystemPrompt("Be brief");
        step.setTemperature("0.5");
        step.setMaxOutputTokens("100");
        step.setTools("shout, other");
        step.setSkills("reviewing");
        step.setQuiet(true);
        p.getBuildersList().add(step);
        p = j.configRoundtrip(p);
        j.assertEqualDataBoundBeans(step, p.getBuildersList().get(ModelRequestExecutionStep.class));
    }

    @Test
    public void freestyleStepExpandsVariablesAndDoesNotShareStateBetweenBuilds() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("WHO", "world")));
        ModelRequestExecutionStep step = new ModelRequestExecutionStep();
        step.setPrompt("Hello ${WHO}");
        step.setSystemPrompt("Be brief");
        step.setTemperature("0.5");
        p.getBuildersList().add(step);

        FreeStyleBuild first = j.buildAndAssertSuccess(p);
        j.assertLogContains("[AI] echo: Hello world", first);
        FreeStyleBuild second = j.buildAndAssertSuccess(p);
        j.assertLogContains("[AI] echo: Hello world", second);
        assertEquals(entries(first), entries(second));
    }

    @Test
    public void freestyleStepRejectsInvalidNumbers() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        ModelRequestExecutionStep step = new ModelRequestExecutionStep();
        step.setPrompt("Hello");
        step.setTemperature("warm");
        p.getBuildersList().add(step);
        j.assertLogContains("temperature must be a number", j.buildAndAssertStatus(Result.FAILURE, p));
    }

    @Test
    public void pipelineStepRejectsOutOfRangeSamplingOptions() {
        AiRequestStep step = new AiRequestStep("x");
        assertThrows(IllegalArgumentException.class, () -> step.setTopP(5.0));
        assertThrows(IllegalArgumentException.class, () -> step.setTopP(0.0));
        assertThrows(IllegalArgumentException.class, () -> step.setTopK(-1));
        assertThrows(IllegalArgumentException.class, () -> step.setTopK(0));
        assertThrows(IllegalArgumentException.class, () -> step.setTemperature(-0.5));
        step.setTopP(1.0);
        step.setTopK(1);
        step.setTemperature(0.0);
    }

    @Test
    public void pipelineStepPassesGenerationOptions() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        String script = """
                def answer = aiRequest(prompt: 'options', thinkingLevel: 'medium', topP: 0.5, topK: 10,
                        stopSequences: ['END'])
                echo "answer=${answer}"
                """;
        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun b = j.buildAndAssertSuccess(p);
        j.assertLogContains("answer=thinking=MEDIUM topP=0.5 topK=10.0 stop=[END]", b);
    }

    @Test
    public void pipelineStepRejectsInvalidOptions() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("aiRequest(prompt: 'hi', thinkingLevel: 'lots')", true));
        j.assertLogContains("Unknown thinking level 'lots'", j.buildAndAssertStatus(Result.FAILURE, p));

        p.setDefinition(new CpsFlowDefinition("aiRequest(prompt: 'hi', outputTypes: ['hologram'])", true));
        j.assertLogContains("Unknown output type 'hologram'", j.buildAndAssertStatus(Result.FAILURE, p));

        p.setDefinition(new CpsFlowDefinition("aiRequest(prompt: 'hi', files: ['notes.txt'])", true));
        j.assertLogContains("aiRequest needs a workspace for files", j.buildAndAssertStatus(Result.FAILURE, p));
    }

    @Test
    public void pipelineStepAttachesFilesAndSavesOutputs() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        String script = """
                node {
                    writeFile file: 'notes.txt', text: 'some notes'
                    def result = aiRequest(prompt: 'draw', files: ['notes.txt'], outputTypes: ['text', 'image'],
                            outputDir: 'out', returnDetails: true)
                    echo "text=${result.text}"
                    echo "files=${result.files.size()} kind=${result.files[0].kind} path=${result.files[0].path}"
                    echo "saved=${fileExists('out/image-1.png')}"
                }
                """;
        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun b = j.buildAndAssertSuccess(p);
        j.assertLogContains("text=drew with notes.txt=some notes", b);
        j.assertLogContains("files=1 kind=image path=out/image-1.png", b);
        j.assertLogContains("saved=true", b);
    }

    @Test
    public void outputTypesSelectAModelThatCanProduceThem() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("aiRequest(prompt: 'hi', outputTypes: ['video'])", true));
        j.assertLogContains("No configured model supports this request", j.buildAndAssertStatus(Result.FAILURE, p));
    }

    @Test
    public void stepTimeoutBoundsTheRequest() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("aiRequest(prompt: 'sleep', timeout: 1)", true));
        j.assertLogContains("timed out after 1 seconds", j.buildAndAssertStatus(Result.FAILURE, p));
    }

    @Test
    public void pipelineStepExpandsEnvironmentVariablesWhenAsked() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        String script = """
                withEnv(['WHO=world']) {
                    def plain = aiRequest('hello ${WHO}')
                    def expanded = aiRequest(prompt: 'hello ${WHO}', expandEnvVars: true)
                    echo "plain=${plain}"
                    echo "expanded=${expanded}"
                }
                """;
        p.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun b = j.buildAndAssertSuccess(p);
        j.assertLogContains("plain=echo: hello ${WHO}", b);
        j.assertLogContains("expanded=echo: hello world", b);
    }

    @Test
    public void executeDoesNotModifyTheRequest() throws Exception {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("hi"));
        request.attachTool(ExtensionList.lookupSingleton(ShoutTool.class));
        RegisteredModel model = ModelRegistry.get().findModelForRequest(request);
        assertNotNull(model);
        assertEquals(
                "tool said HI",
                request.executeOrThrow(
                                model,
                                ModelRequestListener.NOOP,
                                (tool, toolCall) -> tool.callTool(toolCall))
                        .getText());
        assertEquals(2, request.getModelInputs().size());
        assertEquals("tool said HI", request.execute(model).getText());
    }

    private static List<String> entries(FreeStyleBuild build) {
        return build.getAction(ModelConversationsAction.class).getConversations().stream()
                .flatMap(conversation -> conversation.getEntries().stream())
                .map(ConversationEntry::getType)
                .collect(Collectors.toList());
    }

    public static class FakeModelConfiguration extends ModelConfiguration {
        @DataBoundConstructor
        public FakeModelConfiguration(String modelName) throws Descriptor.FormException {
            super(modelName, modelName);
        }

        @Override
        public String getProviderType() {
            return "fake";
        }

        @TestExtension
        public static class DescriptorImpl extends ModelConfiguration.DescriptorImpl {}
    }

    @TestExtension
    public static class FakeClientConfiguration extends ModelClientConfiguration {}

    /**
     * Answers with the prompt text, calls the "shout" tool when tools are attached, and sleeps when asked to.
     */
    @TestExtension
    public static class FakeProvider extends ModelProvider<FakeModelConfiguration> {
        public FakeProvider() {
            super(FakeModelConfiguration.class);
        }

        @Override
        protected ModelData describe(FakeModelConfiguration configuration) {
            ModelData data = new ModelData(configuration);
            data.setInputs(List.of(ModelInputType.TEXT, ModelInputType.FILE));
            data.setOutputs(List.of(ModelOutputType.UNSTRUCTURED_TEXT, ModelOutputType.IMAGE));
            data.setSupportedThinkingLevels(List.of(ModelThinkingLevel.LOW, ModelThinkingLevel.MEDIUM));
            data.setCapabilities(List.of(
                    ModelCapability.TOOLS,
                    ModelCapability.TOKEN_USAGE_METRICS,
                    ModelCapability.ADJUSTABLE_SYSTEM_PROMPT,
                    ModelCapability.CUSTOM_TEMPERATURE,
                    ModelCapability.OUTPUT_TOKEN_LIMITING,
                    ModelCapability.THINKING,
                    ModelCapability.CUSTOM_TOP_P,
                    ModelCapability.CUSTOM_TOP_K,
                    ModelCapability.CUSTOM_STOP_SEQUENCES));
            return data;
        }

        @Override
        protected ModelClient createClient(FakeModelConfiguration configuration) {
            return (modelData, request, turnInputs) -> {
                for (ModelInput input : turnInputs) {
                    if (input instanceof ToolCallResponseContent toolResult) {
                        return List.of(new OutputTextContent("tool said " + toolResult.getResponseContent()));
                    }
                }
                String text = turnInputs.stream()
                        .filter(InputTextContent.class::isInstance)
                        .map(input -> ((InputTextContent) input).getText())
                        .collect(Collectors.joining(" "));
                if (request.getAvailableTools().containsKey("shout")) {
                    return List.of(new ToolCallContent(
                            "call-1", JsonNodeFactory.instance.objectNode().put("text", text), "shout"));
                }
                if (text.equals("sleep")) {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                }
                if (text.equals("options")) {
                    return List.of(new OutputTextContent("thinking="
                            + request.getInput(ThinkingLevelContent.class).getThinkingLevel()
                            + " topP=" + request.getInput(TopPContent.class).getTopP()
                            + " topK=" + request.getInput(TopKContent.class).getTopK()
                            + " stop=" + request.getInput(StopSequencesContent.class).getStopPhrases()));
                }
                String files = turnInputs.stream()
                        .filter(InputFileContent.class::isInstance)
                        .map(input -> (InputFileContent) input)
                        .map(file -> " with " + file.getFileId() + "="
                                + new String(file.getFileData(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining());
                if (text.equals("draw")) {
                    return List.of(
                            new OutputTextContent("drew" + files), new OutputImageContent(new byte[] {1, 2, 3}, "image/png"));
                }
                return List.of(new OutputTextContent("echo: " + text + files), new TokenUtilizationContent(3, 5, 0));
            };
        }

        @Override
        protected long getTimeoutSeconds(FakeModelConfiguration configuration) {
            return ExtensionList.lookupSingleton(FakeClientConfiguration.class).getTimeoutSeconds();
        }
    }

    @TestExtension
    public static class ShoutTool extends Tool {
        public ShoutTool() throws NoSuchMethodException {
            super();
        }

        @Override
        public String getName() {
            return "shout";
        }

        @Override
        public String getDescription() {
            return "Repeats the text in capitals";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of(new ToolArgumentDescription("text", String.class, "The text to repeat", true));
        }

        public String shout(String text) {
            return text.toUpperCase(Locale.ROOT);
        }
    }
}
