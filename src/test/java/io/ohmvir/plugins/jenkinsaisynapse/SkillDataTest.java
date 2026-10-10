package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.DuplicateSkillNameMonitor;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillFileConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class SkillDataTest {

    private static SkillData parse(String frontmatter) throws Exception {
        return new SkillData(Map.of("SKILL.md", "---\n" + frontmatter + "\n---\nSkill body"));
    }

    @Test
    public void missingOptionalFieldsAreNull(JenkinsRule j) throws Exception {
        SkillData skill = parse("name: my-skill");
        assertEquals("my-skill", skill.getSkillName());
        assertNull(skill.getSkillDescription());
        assertNull(skill.getSkillLicense());
        assertNull(skill.getSkillCompatibility());
        assertNull(skill.getSkillMetadata());
        assertNull(skill.getAllowedTools());
    }

    @Test
    public void allowedToolsAreParsed(JenkinsRule j) throws Exception {
        assertEquals(
                List.of("Read", "Bash(git:*)"),
                parse("name: a\nallowed-tools: \"  Read   Bash(git:*) \"").getAllowedTools());
        assertEquals(List.of("Read", "Write"), parse("name: a\nallowed-tools:\n  - Read\n  - Write").getAllowedTools());
        assertEquals(List.of(), parse("name: a\nallowed-tools: \"\"").getAllowedTools());
    }

    @Test
    public void missingNameIsRejected(JenkinsRule j) {
        assertThrows(Descriptor.FormException.class, () -> parse("description: no name"));
    }

    @Test
    public void duplicateSkillNamesAreRejectedAndReported(JenkinsRule j) throws Exception {
        DuplicateSkillNameMonitor monitor = ExtensionList.lookupSingleton(DuplicateSkillNameMonitor.class);
        BasicSkillConfiguration first = new BasicSkillConfiguration("first text", "shared", "first");
        BasicSkillFileConfiguration second =
                new BasicSkillFileConfiguration("---\nname: shared\ndescription: second\n---\nsecond text");
        BasicSkillConfiguration other = new BasicSkillConfiguration("other text", "other", "other");

        SkillData.reloadSkills(List.of(first, second, other));

        assertEquals("first text", SkillData.getSkillData("shared").getSkillText());
        assertEquals(2, SkillData.getAllSkills().size());
        assertEquals(1, SkillData.getDuplicateSkillRegistrations().size());
        SkillData.DuplicateSkillRegistration duplicate = SkillData.getDuplicateSkillRegistrations().get(0);
        assertEquals("shared", duplicate.skillName());
        assertTrue(duplicate.rejectedSource().contains("#2"));
        assertTrue(duplicate.registeredSource().contains("#1"));
        assertTrue(monitor.isActivated());

        // Reloading the same configuration must not treat already-registered skills as duplicates of themselves
        SkillData.reloadSkills(List.of(first, other));
        assertTrue(SkillData.getDuplicateSkillRegistrations().isEmpty());
        assertFalse(monitor.isActivated());
    }
}
