package io.ohmvir.plugins.jenkinsaisynapse.api.tools;

import hudson.ExtensionList;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class ToolRegistry {
    private static @Getter final Map<String, Tool> tools = new HashMap<>();

    /**
     * Extensions are instantiated lazily, so look up every tool declared with @Extension (including the built-in ones)
     * to make sure it is registered before any request needs it.
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void registerExtensionTools() {
        ExtensionList.lookup(Tool.class).forEach(ToolRegistry::register);
    }

    public static void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public static Collection<Tool> getAllTools() {
        return tools.values();
    }

    public static @Nullable Tool getTool(String toolName) {
        return tools.get(toolName);
    }
}
