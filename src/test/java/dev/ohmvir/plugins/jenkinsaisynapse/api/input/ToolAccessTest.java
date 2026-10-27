package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ToolAccessTest {

    public static class AlphaTool extends Tool {
        public AlphaTool() throws NoSuchMethodException {}

        @Override
        public String getName() {
            return "alphaTool";
        }

        @Override
        public String getDescription() {
            return "Alpha";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of();
        }

        public String alphaTool() {
            return "alpha";
        }
    }

    public static class BetaTool extends Tool {
        public BetaTool() throws NoSuchMethodException {}

        @Override
        public String getName() {
            return "betaTool";
        }

        @Override
        public String getDescription() {
            return "Beta";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of();
        }

        public String betaTool() {
            return "beta";
        }
    }

    private static SkillData skill(List<String> allowedTools) {
        return new SkillData("skill", "A skill", null, null, null, allowedTools, "Do things", Map.of());
    }

    @Test
    public void onlyAttachedToolsAreAvailable() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        assertEquals(Set.of("alphaTool"), request.getAvailableTools().keySet());
    }

    @Test
    public void noToolsAreAvailableByDefault() {
        assertEquals(Set.of(), new ModelRequest().getAvailableTools().keySet());
    }

    @Test
    public void skillAllowedToolsDoNotRemoveAttachedTools() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachTool(new BetaTool());
        request.attachSkill(skill(List.of("betaTool", "unattachedTool")));
        assertEquals(Set.of("alphaTool", "betaTool"), request.getAvailableTools().keySet());
    }

    @Test
    public void skillWithoutAllowedToolsKeepsAttachedTools() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachSkill(skill(null));
        request.attachSkill(skill(List.of("")));
        assertEquals(Set.of("alphaTool"), request.getAvailableTools().keySet());
    }

    @Test
    public void attachedToolsSurviveSkillExpansion() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachTool(new BetaTool());
        request.attachSkill(skill(List.of("alphaTool")));
        request.expandSkills();
        assertTrue(request.getModelInputs().stream().noneMatch(InputSkillContent.class::isInstance));
        assertEquals(
                Set.of("alphaTool", "betaTool", ReadSkillFileTool.NAME),
                request.getAvailableTools().keySet());
    }

    @Test
    public void skillToolIsAvailableAfterExpansion() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachSkill(skill(null));
        request.expandSkills();
        assertEquals(
                Set.of("alphaTool", ReadSkillFileTool.NAME),
                request.getAvailableTools().keySet());
    }

    private static SkillData named(String name) {
        return new SkillData(name, "The " + name + " skill", null, null, null, null, "Text of " + name, Map.of());
    }

    private static ReadSkillFileTool skillTool(ModelRequest request) {
        return (ReadSkillFileTool) request.getAvailableTools().get(ReadSkillFileTool.NAME);
    }

    @Test
    public void skillToolOnlyServesAttachedSkills() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachSkill(named("attached"));
        request.expandSkills();
        ReadSkillFileTool tool = skillTool(request);
        assertEquals("Text of attached", tool.read_skill_file("attached", ReadSkillFileTool.SKILL_FILE));
        String refused = tool.read_skill_file("unattached", ReadSkillFileTool.SKILL_FILE);
        assertTrue(refused.startsWith("No skill named 'unattached'"));
        assertTrue(refused.endsWith("Available skills: attached"));
    }

    @Test
    public void skillToolDoesNotServeSkillsWhenNoneAreAttached() throws Exception {
        ReadSkillFileTool tool = new ReadSkillFileTool();
        assertEquals(
                "No skills are available to this request.",
                tool.read_skill_file("anything", ReadSkillFileTool.SKILL_FILE));
    }

    @Test
    public void skillToolServesEveryAttachedSkillThroughOneTool() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachSkill(named("attached"));
        request.attachSkill(named("other"));
        request.expandSkills();
        Set<String> served = skillTool(request).getSkills().stream()
                .map(SkillData::getSkillName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("attached", "other"), served);
        assertEquals(1, request.getModelInputs().stream().filter(InputToolContent.class::isInstance).count());
    }

    @Test
    public void expandingAgainAddsNewSkillsWithoutLosingEarlierOnes() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachSkill(named("first"));
        request.expandSkills();
        request.attachSkill(named("second"));
        request.expandSkills();
        ReadSkillFileTool tool = skillTool(request);
        assertEquals("Text of first", tool.read_skill_file("first", ReadSkillFileTool.SKILL_FILE));
        assertEquals("Text of second", tool.read_skill_file("second", ReadSkillFileTool.SKILL_FILE));
        assertEquals(1, request.getModelInputs().stream().filter(InputToolContent.class::isInstance).count());
    }

    @Test
    public void copiedRequestDoesNotChangeTheOriginalsSkillTool() throws Exception {
        ModelRequest original = new ModelRequest();
        original.attachSkill(named("first"));
        original.expandSkills();
        ModelRequest copy = new ModelRequest(original);
        copy.attachSkill(named("second"));
        copy.expandSkills();
        assertTrue(skillTool(original)
                .read_skill_file("second", ReadSkillFileTool.SKILL_FILE)
                .startsWith("No skill"));
        assertEquals("Text of second", skillTool(copy).read_skill_file("second", ReadSkillFileTool.SKILL_FILE));
        assertEquals("Text of first", skillTool(copy).read_skill_file("first", ReadSkillFileTool.SKILL_FILE));
    }
}
