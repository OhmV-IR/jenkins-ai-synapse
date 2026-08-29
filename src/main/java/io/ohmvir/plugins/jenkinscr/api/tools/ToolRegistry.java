package io.ohmvir.plugins.jenkinscr.api.tools;

import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {
    private static @Getter final Map<String, Tool> tools = new HashMap<>();

    public static void register(Tool tool){
        tools.put(tool.getName(), tool);
    }

    public static Collection<Tool> getAllTools(){
        return tools.values();
    }

    public static @Nullable Tool getTool(String toolName){
        return tools.get(toolName);
    }
}
