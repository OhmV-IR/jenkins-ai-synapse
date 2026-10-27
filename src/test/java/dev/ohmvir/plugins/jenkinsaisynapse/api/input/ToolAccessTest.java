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
    public void skillAllowedToolsRestrictAttachedTools() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachTool(new BetaTool());
        request.attachSkill(skill(List.of("betaTool", "unattachedTool")));
        assertEquals(Set.of("betaTool"), request.getAvailableTools().keySet());
    }

    @Test
    public void skillWithoutAllowedToolsDoesNotRestrict() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachSkill(skill(null));
        request.attachSkill(skill(List.of("")));
        assertEquals(Set.of("alphaTool"), request.getAvailableTools().keySet());
    }

    @Test
    public void skillToolStaysAvailableWhenSkillsRestrictTools() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachTool(new BetaTool());
        request.attachTool(new ReadSkillFileTool());
        request.attachSkill(skill(List.of("alphaTool")));
        assertEquals(
                Set.of("alphaTool", ReadSkillFileTool.NAME),
                request.getAvailableTools().keySet());
    }

    @Test
    public void skillRestrictionSurvivesSkillExpansion() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachTool(new BetaTool());
        // Attached up front so expansion does not look the tool up from Jenkins
        request.attachTool(new ReadSkillFileTool());
        request.attachSkill(skill(List.of("alphaTool")));
        request.expandSkills();
        assertTrue(request.getModelInputs().stream().noneMatch(InputSkillContent.class::isInstance));
        assertEquals(
                Set.of("alphaTool", ReadSkillFileTool.NAME),
                request.getAvailableTools().keySet());
    }

    @Test
    public void skillToolIsAvailableAfterExpansionWithoutRestriction() throws Exception {
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        request.attachTool(new ReadSkillFileTool());
        request.attachSkill(skill(null));
        request.expandSkills();
        assertEquals(
                Set.of("alphaTool", ReadSkillFileTool.NAME),
                request.getAvailableTools().keySet());
    }
}
