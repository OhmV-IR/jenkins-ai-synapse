package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import org.junit.jupiter.api.Test;

public class ModelCapabilityTest {
    @Test
    public void wellKnownIdsResolveToConstants() {
        assertSame(ModelCapability.TOOLS, ModelCapability.of("TOOLS"));
        assertSame(ModelInputType.IMAGE, ModelInputType.of("IMAGE"));
        assertSame(ModelOutputType.EMBEDDINGS, ModelOutputType.of("EMBEDDINGS"));
        assertTrue(ModelCapability.TOOLS.isWellKnown());
        assertTrue(ModelCapability.wellKnownValues().contains(ModelCapability.TOKEN_USAGE_METRICS));
        assertTrue(ModelCapability.wellKnownValues().contains(ModelCapability.DECISIONS));
        assertTrue(ModelCapability.wellKnownValues().contains(ModelCapability.INFILL));
        assertEquals(18, ModelCapability.wellKnownValues().size());
    }

    @Test
    public void providersCanDefineNamespacedValues() {
        ModelCapability computerUse = ModelCapability.of("anthropic:computer-use");
        assertEquals("anthropic:computer-use", computerUse.getId());
        assertFalse(computerUse.isWellKnown());
        assertEquals(computerUse, ModelCapability.of("anthropic:computer-use"));
        assertEquals(
                computerUse.hashCode(),
                ModelCapability.of("anthropic:computer-use").hashCode());
        assertNotEquals(computerUse, ModelCapability.of("openai:computer-use"));
        assertEquals(ModelInputType.of("com.example:pdf"), ModelInputType.of("com.example:pdf"));
    }

    @Test
    public void sameIdOfDifferentKindsIsNotEqual() {
        assertNotEquals(ModelInputType.IMAGE, ModelOutputType.IMAGE);
        assertNotEquals(ModelInputType.of("vendor:pdf"), ModelOutputType.of("vendor:pdf"));
    }

    @Test
    public void rejectsUnknownBareAndMalformedIds() {
        for (String id :
                new String[] {null, "", "COMPUTER_USE", "computer-use", ":pdf", "vendor:", "Vendor:pdf", "a b:c"}) {
            assertThrows(IllegalArgumentException.class, () -> ModelCapability.of(id), String.valueOf(id));
        }
    }
}
