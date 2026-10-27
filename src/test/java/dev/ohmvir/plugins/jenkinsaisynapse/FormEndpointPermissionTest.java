package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.FullPrompt;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.DeclaredSkillConfiguration;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.function.Supplier;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.springframework.security.access.AccessDeniedException;

/**
 * The form validation and fill endpoints of the administrator-only configuration forms must refuse callers without
 * {@link Jenkins#ADMINISTER}, so that they do not disclose configuration to users with only Overall/Read.
 */
@WithJenkins
public class FormEndpointPermissionTest {
    private JenkinsRule j;

    @BeforeEach
    public void setUp(JenkinsRule j) {
        this.j = j;
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin")
                .grant(Jenkins.READ)
                .everywhere()
                .to("reader"));
    }

    private static <T> T asUser(String user, Supplier<T> action) {
        try (ACLContext ignored = ACL.as2(User.getById(user, true).impersonate2())) {
            return action.get();
        }
    }

    private static void assertDenied(Supplier<?> action) {
        assertThrows(AccessDeniedException.class, () -> asUser("reader", action));
    }

    private static void assertDeniedForReaderOnly(Supplier<FormValidation> check, FormValidation.Kind adminKind) {
        assertDenied(check);
        assertEquals(adminKind, asUser("admin", check).kind);
    }

    @Test
    public void promptCheckMethodsRequireAdministerPermission() {
        FullPrompt.DescriptorImpl full = j.jenkins.getDescriptorByType(FullPrompt.DescriptorImpl.class);
        SimplePrompt.DescriptorImpl simple = j.jenkins.getDescriptorByType(SimplePrompt.DescriptorImpl.class);

        assertDeniedForReaderOnly(() -> full.doCheckPromptId(""), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> full.doCheckPromptId("id"), FormValidation.Kind.OK);
        assertDeniedForReaderOnly(() -> simple.doCheckPromptText(""), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> simple.doCheckPromptText("text"), FormValidation.Kind.OK);
        assertDeniedForReaderOnly(() -> full.doCheckSystemPrompt(""), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> full.doCheckSystemPrompt("system"), FormValidation.Kind.OK);
        assertDeniedForReaderOnly(() -> full.doCheckTemperature(-1), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> full.doCheckMaxOutputTokensCount(-1), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> full.doCheckMaxOutputTokensCount(0), FormValidation.Kind.OK);
        assertDeniedForReaderOnly(() -> full.doCheckThinkingLevel(null), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> full.doCheckThinkingLevel(ModelThinkingLevel.OFF), FormValidation.Kind.OK);
    }

    @Test
    public void promptCheckMethodsTreatMissingValuesAsErrors() {
        FullPrompt.DescriptorImpl full = j.jenkins.getDescriptorByType(FullPrompt.DescriptorImpl.class);
        SimplePrompt.DescriptorImpl simple = j.jenkins.getDescriptorByType(SimplePrompt.DescriptorImpl.class);

        assertEquals(FormValidation.Kind.ERROR, asUser("admin", () -> full.doCheckPromptId(null)).kind);
        assertEquals(FormValidation.Kind.ERROR, asUser("admin", () -> simple.doCheckPromptText(null)).kind);
        assertEquals(FormValidation.Kind.ERROR, asUser("admin", () -> full.doCheckSystemPrompt(null)).kind);
    }

    @Test
    public void declaredSkillCheckMethodsRequireAdministerPermission() {
        DeclaredSkillConfiguration.DescriptorImpl skill =
                j.jenkins.getDescriptorByType(DeclaredSkillConfiguration.DescriptorImpl.class);
        DeclaredSkillConfiguration.SkillReferenceEntry.DescriptorImpl reference =
                j.jenkins.getDescriptorByType(DeclaredSkillConfiguration.SkillReferenceEntry.DescriptorImpl.class);
        DeclaredSkillConfiguration.AllowedTool.DescriptorImpl tool =
                j.jenkins.getDescriptorByType(DeclaredSkillConfiguration.AllowedTool.DescriptorImpl.class);

        assertDeniedForReaderOnly(() -> skill.doCheckSkillMetadata("key: value"), FormValidation.Kind.OK);
        assertDeniedForReaderOnly(() -> skill.doCheckSkillMetadata("- a\n- b"), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> reference.doCheckRelativeFilePath(""), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> reference.doCheckRelativeFilePath("a.txt"), FormValidation.Kind.OK);
        assertDeniedForReaderOnly(() -> reference.doCheckFileContent(""), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> reference.doCheckFileContent("content"), FormValidation.Kind.OK);
        assertDeniedForReaderOnly(() -> tool.doCheckToolName(""), FormValidation.Kind.ERROR);
        assertDeniedForReaderOnly(() -> tool.doCheckToolName("tool"), FormValidation.Kind.OK);
    }

    @Test
    public void skillMetadataCheckAcceptsNoMetadataAndRejectsNonMappings() {
        DeclaredSkillConfiguration.DescriptorImpl skill =
                j.jenkins.getDescriptorByType(DeclaredSkillConfiguration.DescriptorImpl.class);

        assertEquals(FormValidation.Kind.OK, asUser("admin", () -> skill.doCheckSkillMetadata(null)).kind);
        assertEquals(FormValidation.Kind.OK, asUser("admin", () -> skill.doCheckSkillMetadata("  ")).kind);
        assertEquals(FormValidation.Kind.ERROR, asUser("admin", () -> skill.doCheckSkillMetadata("scalar")).kind);
        assertEquals(FormValidation.Kind.ERROR, asUser("admin", () -> skill.doCheckSkillMetadata("[a, b]")).kind);
    }

    @Test
    public void toolNameItemsRequireAdministerPermission() {
        DeclaredSkillConfiguration.AllowedTool.DescriptorImpl tool =
                j.jenkins.getDescriptorByType(DeclaredSkillConfiguration.AllowedTool.DescriptorImpl.class);

        assertDenied(tool::doFillToolNameItems);
        ListBoxModel items = asUser("admin", tool::doFillToolNameItems);
        assertEquals(ToolRegistry.getAllTools().size(), items.size());
    }
}
