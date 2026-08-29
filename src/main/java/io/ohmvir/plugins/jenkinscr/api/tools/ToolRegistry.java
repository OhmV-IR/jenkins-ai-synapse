package io.ohmvir.plugins.jenkinscr.api.tools;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {
    private static @Getter final Map<String, Tool> tools = new HashMap<>();

    public static void register(Tool tool){
        tools.put(tool.getName(), tool);
    }
}
