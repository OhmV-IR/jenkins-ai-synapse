package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InfillPrefixContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InfillSuffixContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class InfillContentTest {
    private static final String PREFIX = "def add(a, b):\n    ";
    private static final String SUFFIX = "\n\nprint(add(1, 2))";

    @Test
    public void infillRequestRequiresInfillCapability(JenkinsRule j) {
        ModelRequest request = new ModelRequest();
        request.addInput(new InfillPrefixContent(PREFIX));
        request.addInput(new InfillSuffixContent(SUFFIX));

        assertEquals(Set.of(ModelCapability.INFILL), request.getRequiredCapabilities());
        assertEquals(Set.of(ModelInputType.TEXT), request.getInputTypes());
        assertEquals(PREFIX, request.getInput(InfillPrefixContent.class).getPrefix());
        assertEquals(SUFFIX, request.getInput(InfillSuffixContent.class).getSuffix());
    }
}
