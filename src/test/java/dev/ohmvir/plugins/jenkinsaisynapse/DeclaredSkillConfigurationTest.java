package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.DeclaredSkillConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.DeclaredSkillConfiguration.SkillReferenceEntry;
import hudson.model.Descriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DeclaredSkillConfigurationTest {
    private static DeclaredSkillConfiguration config(String metadata, List<SkillReferenceEntry> references)
            throws Descriptor.FormException {
        return new DeclaredSkillConfiguration(
                "my-skill", "A skill", null, null, metadata, List.of(), "Skill body", references);
    }

    private static SkillData generate(String metadata) throws Descriptor.FormException {
        List<SkillData> skills = config(metadata, List.of()).generateSkills();
        assertEquals(1, skills.size());
        return skills.get(0);
    }

    @Test
    public void nullMetadataMeansNoMetadata() throws Exception {
        assertNull(generate(null).getSkillMetadata());
    }

    @Test
    public void blankMetadataMeansNoMetadata() throws Exception {
        assertNull(generate("").getSkillMetadata());
        assertNull(generate("  \n\t ").getSkillMetadata());
        assertNull(generate("# only a comment\n").getSkillMetadata());
    }

    @Test
    public void mappingMetadataIsParsed() throws Exception {
        SkillData skill = generate("author: me\nversion: \"1.0\"\n");
        assertTrue(skill.getSkillMetadata().isObject());
        assertEquals("me", skill.getSkillMetadata().get("author").asText());
        assertEquals("1.0", skill.getSkillMetadata().get("version").asText());
    }

    @Test
    public void scalarMetadataIsRejected() {
        Descriptor.FormException e =
                assertThrows(Descriptor.FormException.class, () -> config("just some text", List.of()));
        assertEquals("skillMetadata", e.getFormField());
        assertTrue(e.getMessage().contains("YAML mapping"));
    }

    @Test
    public void listMetadataIsRejected() {
        Descriptor.FormException e =
                assertThrows(Descriptor.FormException.class, () -> config("- one\n- two\n", List.of()));
        assertEquals("skillMetadata", e.getFormField());
        assertTrue(e.getMessage().contains("YAML mapping"));
    }

    @Test
    public void invalidYamlMetadataIsRejected() {
        Descriptor.FormException e =
                assertThrows(Descriptor.FormException.class, () -> config("key: [unclosed", List.of()));
        assertEquals("skillMetadata", e.getFormField());
    }

    @Test
    public void storedMetadataStringIsKeptAsEntered() throws Exception {
        assertNull(config(null, List.of()).getSkillMetadata());
        assertEquals("author: me", config("author: me", List.of()).getSkillMetadata());
    }

    @Test
    public void distinctReferencesAreKept() throws Exception {
        List<SkillData> skills = config(
                        null,
                        List.of(
                                new SkillReferenceEntry("references/a.md", "A"),
                                new SkillReferenceEntry("references/b.md", "B")))
                .generateSkills();
        assertEquals(1, skills.size());
        assertEquals(2, skills.get(0).getSkillReferences().size());
        assertEquals("A", skills.get(0).getSkillReferences().get("references/a.md"));
        assertEquals("B", skills.get(0).getSkillReferences().get("references/b.md"));
    }

    @Test
    public void duplicateReferencePathsAreRejected() throws Exception {
        List<SkillReferenceEntry> references = List.of(
                new SkillReferenceEntry("references/a.md", "first"),
                new SkillReferenceEntry("references/b.md", "other"),
                new SkillReferenceEntry("references/a.md", "second"));
        Descriptor.FormException e = assertThrows(Descriptor.FormException.class, () -> config(null, references));
        assertEquals("skillReferences", e.getFormField());
        assertTrue(e.getMessage().contains("references/a.md"));
    }

    @Test
    public void referenceNamedSkillMdIsRejected() throws Exception {
        List<SkillReferenceEntry> references = List.of(new SkillReferenceEntry("SKILL.md", "content"));
        Descriptor.FormException e = assertThrows(Descriptor.FormException.class, () -> config(null, references));
        assertEquals("skillReferences", e.getFormField());
        assertTrue(e.getMessage().contains("SKILL.md"));
    }

    @Test
    public void nullReferencesAreAllowed() throws Exception {
        assertTrue(config(null, null).getSkillReferences().isEmpty());
    }
}
