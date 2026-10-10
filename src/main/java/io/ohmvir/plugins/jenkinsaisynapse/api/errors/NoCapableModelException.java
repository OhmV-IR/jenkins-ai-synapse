package io.ohmvir.plugins.jenkinsaisynapse.api.errors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thrown when no configured model can serve a request: none supports everything it needs, or the model asked for is
 * not configured.
 */
public class NoCapableModelException extends ModelException {
    private final Map<String, List<String>> unsupportedRequirements;

    /**
     * @param message What went wrong.
     * @param unsupportedRequirements For each model considered, the requirements of the request it does not meet.
     */
    public NoCapableModelException(String message, Map<String, List<String>> unsupportedRequirements) {
        super(message);
        this.unsupportedRequirements = Collections.unmodifiableMap(new LinkedHashMap<>(unsupportedRequirements));
    }

    public Map<String, List<String>> getUnsupportedRequirements() {
        return unsupportedRequirements;
    }
}
