package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.DuplicateSkillNameMonitor;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillFileConfiguration;
import hudson.ExtensionList;
import hudson.model.Descriptor;
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
        assertEquals(
                List.of("Read", "Write"),
                parse("name: a\nallowed-tools:\n  - Read\n  - Write").getAllowedTools());
        assertEquals(List.of(), parse("name: a\nallowed-tools: \"\"").getAllowedTools());
    }

    @Test
    public void missingNameIsRejected(JenkinsRule j) {
        assertThrows(Descriptor.FormException.class, () -> parse("description: no name"));
    }

    private static SkillData parseFile(String skillMd) throws Exception {
        return new SkillData(Map.of("SKILL.md", skillMd));
    }

    @Test
    public void textBeforeFrontmatterMeansNoFrontmatter(JenkinsRule j) {
        assertThrows(
                Descriptor.FormException.class,
                () -> parseFile("Some intro\n---\nname: my-skill\n---\nSkill body"));
    }

    @Test
    public void horizontalRuleInBodyWithoutFrontmatterIsNotFrontmatter(JenkinsRule j) {
        assertThrows(
                Descriptor.FormException.class,
                () -> parseFile("# Title\n\n---\n\nname: my-skill\n\n---\n\nMore text"));
    }

    @Test
    public void horizontalRuleInBodyIsKeptInSkillText(JenkinsRule j) throws Exception {
        SkillData skill = parseFile("---\nname: my-skill\n---\nFirst\n\n---\n\nSecond");
        assertEquals("my-skill", skill.getSkillName());
        assertEquals("First\n\n---\n\nSecond", skill.getSkillText());
    }

    @Test
    public void delimiterInsideFrontmatterValueIsNotTheClosingDelimiter(JenkinsRule j) throws Exception {
        SkillData skill = parseFile("---\nname: my-skill\ndescription: \"a---b\"\n---\nSkill body");
        assertEquals("my-skill", skill.getSkillName());
        assertEquals("a---b", skill.getSkillDescription());
        assertEquals("Skill body", skill.getSkillText());
    }

    @Test
    public void crlfLineEndingsAreSupported(JenkinsRule j) throws Exception {
        SkillData skill = parseFile("---\r\nname: my-skill\r\ndescription: crlf\r\n---\r\nSkill body\r\n");
        assertEquals("my-skill", skill.getSkillName());
        assertEquals("crlf", skill.getSkillDescription());
        assertEquals("Skill body\r\n", skill.getSkillText());
    }

    @Test
    public void byteOrderMarkBeforeFrontmatterIsTolerated(JenkinsRule j) throws Exception {
        SkillData skill = parseFile("\uFEFF---\nname: my-skill\n---\nSkill body");
        assertEquals("my-skill", skill.getSkillName());
        assertEquals("Skill body", skill.getSkillText());
    }

    @Test
    public void emptyFrontmatterBlockIsParsedAndRejectedForMissingName(JenkinsRule j) {
        // The block is recognised as frontmatter (not reported as an invalid file) but holds no mapping with a name
        Descriptor.FormException e =
                assertThrows(Descriptor.FormException.class, () -> parseFile("---\n---\nSkill body"));
        assertTrue(e.getMessage().contains("YAML mapping"), e.getMessage());
    }

    @Test
    public void trailingWhitespaceOnDelimiterLinesIsTolerated(JenkinsRule j) throws Exception {
        SkillData skill = parseFile("---  \nname: my-skill\n---\t\nSkill body");
        assertEquals("my-skill", skill.getSkillName());
        assertEquals("Skill body", skill.getSkillText());
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
        SkillData.DuplicateSkillRegistration duplicate =
                SkillData.getDuplicateSkillRegistrations().get(0);
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
