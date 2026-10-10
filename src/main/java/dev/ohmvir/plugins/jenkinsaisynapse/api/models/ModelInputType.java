package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A kind of content a model accepts as input. The constants are the well-known input types understood by the core
 * plugin; provider plugins can define additional ones with {@link #of(String)} and a namespaced id such as
 * {@code vendor:pdf}.
 */
public final class ModelInputType extends NamespacedId {
    private static final long serialVersionUID = 1L;
    private static final Map<String, ModelInputType> WELL_KNOWN = new LinkedHashMap<>();

    public static final ModelInputType TEXT = wellKnown("TEXT");
    public static final ModelInputType IMAGE = wellKnown("IMAGE");
    public static final ModelInputType VIDEO = wellKnown("VIDEO");
    public static final ModelInputType AUDIO = wellKnown("AUDIO");
    public static final ModelInputType FILE = wellKnown("FILE");

    private ModelInputType(String id) {
        super(id);
    }

    private static ModelInputType wellKnown(String id) {
        ModelInputType type = new ModelInputType(requireWellKnownId(id));
        WELL_KNOWN.put(id, type);
        return type;
    }

    /**
     * @param id A well-known id (eg {@code TEXT}) or a namespaced id (eg {@code vendor:pdf})
     * @return The matching type
     * @throws IllegalArgumentException If the id is neither a well-known id nor a valid namespaced id
     */
    public static ModelInputType of(String id) {
        ModelInputType known = WELL_KNOWN.get(id);
        return known != null ? known : new ModelInputType(requireNamespacedId(id));
    }

    public static Collection<ModelInputType> wellKnownValues() {
        return Collections.unmodifiableCollection(WELL_KNOWN.values());
    }

    private Object readResolve() {
        return of(getId());
    }
}
