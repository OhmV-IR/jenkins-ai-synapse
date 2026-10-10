package io.ohmvir.plugins.jenkinsaisynapse;

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
import io.ohmvir.plugins.jenkinsaisynapse.api.ConversationEntry;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversationsAction;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputToolContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelDataRetriever;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.client.ModelClientConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.File;
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
        assertFalse(buildXml.contains("<io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent>"), buildXml);
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
    public void executeDoesNotModifyTheRequest() throws Exception {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("hi"));
        request.attachTool(ExtensionList.lookupSingleton(ShoutTool.class));
        ModelData model = ModelData.get("fake:echo");
        assertEquals("tool said HI", request.executeOrThrow(model, null).getText());
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
    public static class FakeClient extends ModelClient<FakeModelConfiguration, FakeClientConfiguration> {
        @Override
        protected List<ModelOutput> takeStepImpl(
                ModelData modelData,
                FakeModelConfiguration configuration,
                FakeClientConfiguration clientConfiguration,
                ModelRequest request,
                List<ModelInput> turnInputs) {
            for (ModelInput input : turnInputs) {
                if (input instanceof ToolCallResponseContent toolResult) {
                    return List.of(new OutputTextContent("tool said " + toolResult.getResponseContent()));
                }
            }
            String text = turnInputs.stream()
                    .filter(InputTextContent.class::isInstance)
                    .map(input -> ((InputTextContent) input).getText())
                    .collect(Collectors.joining(" "));
            if (turnInputs.stream().anyMatch(InputToolContent.class::isInstance)) {
                return List.of(new ToolCallContent(
                        "call-1", JsonNodeFactory.instance.objectNode().put("text", text), "shout"));
            }
            if (text.equals("sleep")) {
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
            return List.of(new OutputTextContent("echo: " + text), new TokenUtilizationContent(3, 5, 0));
        }
    }

    @TestExtension
    public static class FakeRetriever extends ModelDataRetriever<FakeModelConfiguration> {
        public FakeRetriever() {
            super(FakeModelConfiguration.class);
        }

        @Override
        public ModelData retrieveFromConfiguration(FakeModelConfiguration configuration) {
            ModelData data = new ModelData(configuration);
            data.setInputs(List.of(ModelInputType.TEXT));
            data.setOutputs(List.of(ModelOutputType.UNSTRUCTURED_TEXT));
            data.setCapabilities(List.of(
                    ModelCapability.TOOLS,
                    ModelCapability.TOKEN_USAGE_METRICS,
                    ModelCapability.ADJUSTABLE_SYSTEM_PROMPT,
                    ModelCapability.CUSTOM_TEMPERATURE,
                    ModelCapability.OUTPUT_TOKEN_LIMITING));
            return data;
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
