package io.ohmvir.plugins.jenkinscr.configuration.agents;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.util.ListBoxModel;

public class AgentReasoningLevel {
    public static AgentReasoningLevel LOW = new AgentReasoningLevel("low");
    public static AgentReasoningLevel MEDIUM = new AgentReasoningLevel("medium");
    public static AgentReasoningLevel HIGH = new AgentReasoningLevel("high");
    public static AgentReasoningLevel MAX = new AgentReasoningLevel("max");

    public String levelString;

    public AgentReasoningLevel(String levelString){
        this.levelString = levelString;
    }

    public static AgentReasoningLevel fromString(String level){
        if(level == null) return null;
        switch (level) {
            case "low" -> {
                return LOW;
            }
            case "medium" -> {
                return MEDIUM;
            }
            case "high" -> {
                return HIGH;
            }
            case "max" -> {
                return MAX;
            }
            default -> {
                return null;
            }
        }
    }

    public String toString(){
        return levelString;
    }

    // TODO IMPLEMENT
    public boolean modelSupportsLevel(String modelId){
        return true;
    }

    // TODO gpt-oss only low, med, high. Others all 4.
    public static ListBoxModel GetReasoningOptions(String modelId){
        var list = new StandardListBoxModel();
        list.add(LOW.levelString, LOW.levelString);
        list.add(MEDIUM.levelString, MEDIUM.levelString);
        list.add(HIGH.levelString, HIGH.levelString);
        if(!modelId.contains("gpt-oss")){
            list.add(MAX.levelString, MAX.levelString);
        }
        return list;
    }
}