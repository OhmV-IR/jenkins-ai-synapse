package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A kind of content a model can produce. The constants are the well-known output types understood by the core plugin;
 * provider plugins can define additional ones with {@link #of(String)} and a namespaced id such as
 * {@code vendor:3d-model}.
 */
public final class ModelOutputType extends NamespacedId {
    private static final long serialVersionUID = 1L;
    private static final Map<String, ModelOutputType> WELL_KNOWN = new LinkedHashMap<>();

    public static final ModelOutputType UNSTRUCTURED_TEXT = wellKnown("UNSTRUCTURED_TEXT");
    public static final ModelOutputType STRUCTURED_OUTPUT = wellKnown("STRUCTURED_OUTPUT");
    public static final ModelOutputType IMAGE = wellKnown("IMAGE");
    public static final ModelOutputType VIDEO = wellKnown("VIDEO");
    public static final ModelOutputType AUDIO = wellKnown("AUDIO");
    public static final ModelOutputType EMBEDDINGS = wellKnown("EMBEDDINGS");
    public static final ModelOutputType FILE = wellKnown("FILE");

    private ModelOutputType(String id) {
        super(id);
    }

    private static ModelOutputType wellKnown(String id) {
        ModelOutputType type = new ModelOutputType(requireWellKnownId(id));
        WELL_KNOWN.put(id, type);
        return type;
    }

    /**
     * @param id A well-known id (eg {@code IMAGE}) or a namespaced id (eg {@code vendor:3d-model})
     * @return The matching type
     * @throws IllegalArgumentException If the id is neither a well-known id nor a valid namespaced id
     */
    public static ModelOutputType of(String id) {
        ModelOutputType known = WELL_KNOWN.get(id);
        return known != null ? known : new ModelOutputType(requireNamespacedId(id));
    }

    public static Collection<ModelOutputType> wellKnownValues() {
        return Collections.unmodifiableCollection(WELL_KNOWN.values());
    }

    private Object readResolve() {
        return of(getId());
    }
}
