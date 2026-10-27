package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import hudson.EnvVars;
import hudson.Util;
import org.jspecify.annotations.Nullable;

/**
 * Expands references to environment variables in step parameters. A value that is empty after expansion, for example
 * because {@code ${EXTRA}} is empty, is treated like a value that was never set: the result is {@code null}.
 */
public final class EnvVarExpansion {
    private EnvVarExpansion() {}

    /**
     * @return The expanded value, or {@code null} when {@code value} is {@code null} or expands to an empty string
     */
    public static @Nullable String expand(@Nullable EnvVars env, @Nullable String value) {
        return value == null ? null : Util.fixEmpty(env == null ? value : env.expand(value));
    }

    /**
     * @return The expanded value without surrounding whitespace, or {@code null} when {@code value} is {@code null}
     *         or expands to a blank string
     */
    public static @Nullable String expandAndTrim(@Nullable EnvVars env, @Nullable String value) {
        return value == null ? null : Util.fixEmptyAndTrim(env == null ? value : env.expand(value));
    }
}
