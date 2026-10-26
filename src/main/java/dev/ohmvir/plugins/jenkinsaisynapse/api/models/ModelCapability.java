package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A feature a model can have. The constants are the well-known capabilities understood by the core plugin; provider
 * plugins can define additional ones with {@link #of(String)} and a namespaced id such as {@code anthropic:computer-use}.
 */
public final class ModelCapability extends NamespacedId {
    private static final long serialVersionUID = 1L;
    private static final Map<String, ModelCapability> WELL_KNOWN = new LinkedHashMap<>();

    public static final ModelCapability STREAMING = wellKnown("STREAMING");
    public static final ModelCapability TOOLS = wellKnown("TOOLS");
    public static final ModelCapability SKILLS = wellKnown("SKILLS");
    public static final ModelCapability WEB_SEARCH = wellKnown("WEB_SEARCH");
    public static final ModelCapability CODE_EXECUTION = wellKnown("CODE_EXECUTION");
    public static final ModelCapability CITATIONS = wellKnown("CITATIONS");
    public static final ModelCapability ADJUSTABLE_SYSTEM_PROMPT = wellKnown("ADJUSTABLE_SYSTEM_PROMPT");
    public static final ModelCapability THINKING = wellKnown("THINKING");
    public static final ModelCapability CONVERSATIONS = wellKnown("CONVERSATIONS");
    public static final ModelCapability OUTPUT_TOKEN_LIMITING = wellKnown("OUTPUT_TOKEN_LIMITING");
    public static final ModelCapability CUSTOM_TEMPERATURE = wellKnown("CUSTOM_TEMPERATURE");
    public static final ModelCapability PREMATURE_STOP = wellKnown("PREMATURE_STOP");
    public static final ModelCapability CUSTOM_TOP_P = wellKnown("CUSTOM_TOP_P");
    public static final ModelCapability CUSTOM_TOP_K = wellKnown("CUSTOM_TOP_K");
    public static final ModelCapability CUSTOM_STOP_SEQUENCES = wellKnown("CUSTOM_STOP_SEQUENCES");
    public static final ModelCapability TOKEN_USAGE_METRICS = wellKnown("TOKEN_USAGE_METRICS");
    public static final ModelCapability INFILL = wellKnown("INFILL");
    /** Answers typed questions with probabilities instead of generating text. */
    public static final ModelCapability DECISIONS = wellKnown("DECISIONS");

    private ModelCapability(String id) {
        super(id);
    }

    private static ModelCapability wellKnown(String id) {
        ModelCapability capability = new ModelCapability(requireWellKnownId(id));
        WELL_KNOWN.put(id, capability);
        return capability;
    }

    /**
     * @param id A well-known id (eg {@code TOOLS}) or a namespaced id (eg {@code vendor:computer-use})
     * @return The matching capability
     * @throws IllegalArgumentException If the id is neither a well-known id nor a valid namespaced id
     */
    public static ModelCapability of(String id) {
        ModelCapability known = WELL_KNOWN.get(id);
        return known != null ? known : new ModelCapability(requireNamespacedId(id));
    }

    public static Collection<ModelCapability> wellKnownValues() {
        return Collections.unmodifiableCollection(WELL_KNOWN.values());
    }

    private Object readResolve() {
        return of(getId());
    }
}
