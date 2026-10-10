package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
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
        new BetaTool(); // registered, but not attached
        ModelRequest request = new ModelRequest();
        request.attachTool(new AlphaTool());
        assertEquals(Set.of("alphaTool"), request.getAvailableTools().keySet());
    }

    @Test
    public void noToolsAreAvailableByDefault() throws Exception {
        new AlphaTool();
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
    public void allowedToolsAreParsedFromSkillFile() throws Exception {
        SkillData spaced = new SkillData(
                Map.of("SKILL.md", "---\nname: s\ndescription: d\nallowed-tools: alphaTool  betaTool\n---\nBody"));
        assertEquals(List.of("alphaTool", "betaTool"), spaced.getAllowedTools());

        SkillData listed = new SkillData(
                Map.of("SKILL.md", "---\nname: s\ndescription: d\nallowed-tools:\n  - alphaTool\n---\nBody"));
        assertEquals(List.of("alphaTool"), listed.getAllowedTools());

        SkillData missing = new SkillData(Map.of("SKILL.md", "---\nname: s\ndescription: d\n---\nBody"));
        assertEquals(List.of(), missing.getAllowedTools());
    }
}
