package io.ohmvir.plugins.jenkinsaisynapse.api.tools;

import hudson.ExtensionList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Looks up the tools registered as Jenkins extensions.
 */
public class ToolRegistry {
    public static List<Tool> getAllTools() {
        return ExtensionList.lookup(Tool.class);
    }

    public static @Nullable Tool getTool(String toolName) {
        return getAllTools().stream()
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }
}
