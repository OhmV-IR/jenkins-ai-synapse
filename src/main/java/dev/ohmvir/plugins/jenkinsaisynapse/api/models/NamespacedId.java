package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Base for open-ended identifiers such as {@link ModelCapability}, {@link ModelInputType} and {@link ModelOutputType}.
 * The core plugin defines well-known values with bare upper-case ids (eg {@code TOOLS}). Provider plugins can define
 * their own values with a namespaced id of the form {@code namespace:name} (eg {@code anthropic:computer-use} or
 * {@code com.example:pdf}), so they never collide with well-known values or with each other.
 * Two values are equal when they are of the same type and have the same id.
 */
public abstract class NamespacedId implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Pattern WELL_KNOWN_ID = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern NAMESPACED_ID = Pattern.compile("[a-z0-9][a-z0-9._-]*:[A-Za-z0-9][A-Za-z0-9._-]*");

    private final String id;

    protected NamespacedId(String id) {
        this.id = id;
    }

    public final String getId() {
        return id;
    }

    /**
     * @return true if this value is defined by the core plugin, false if it was defined by an extension.
     */
    public final boolean isWellKnown() {
        return WELL_KNOWN_ID.matcher(id).matches();
    }

    static String requireWellKnownId(String id) {
        if (id == null || !WELL_KNOWN_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Well-known ids must be UPPER_SNAKE_CASE, got: " + id);
        }
        return id;
    }

    static String requireNamespacedId(String id) {
        if (id == null || !NAMESPACED_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "Custom ids must be namespaced as namespace:name (eg vendor:computer-use), got: " + id);
        }
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && id.equals(((NamespacedId) o).id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public final String toString() {
        return id;
    }
}
