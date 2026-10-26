package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.DecisionChoiceQuestionContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.DecisionScoreQuestionContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.DecisionYesNoQuestionContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.DecisionChoiceAnswerContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.DecisionScoreAnswerContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.DecisionYesNoAnswerContent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class DecisionContentTest {
    private static Map<String, String> severityOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("low", "Cosmetic, no user impact");
        options.put("medium", "Degrades a feature");
        options.put("high", "Breaks a feature or loses data");
        return options;
    }

    @Test
    public void keepsQuestionFields() {
        DecisionYesNoQuestionContent yesNo =
                new DecisionYesNoQuestionContent("destructive", "Would this destroy data?", "It deletes data", null);
        assertEquals("destructive", yesNo.getName());
        assertEquals("Would this destroy data?", yesNo.getInstructions());
        assertEquals("It deletes data", yesNo.getTrueCriteria());

        DecisionChoiceQuestionContent choice =
                new DecisionChoiceQuestionContent("severity", "How severe is this failure?", severityOptions());
        assertEquals(
                List.of("low", "medium", "high"),
                List.copyOf(choice.getCriteria().keySet()));

        DecisionScoreQuestionContent score =
                new DecisionScoreQuestionContent("quality", null, List.of("Unusable", "Needs work", "Ready"));
        assertEquals("Ready", score.getLevels().get(2));
    }

    @Test
    public void rejectsInvalidQuestions() {
        assertThrows(IllegalArgumentException.class, () -> new DecisionYesNoQuestionContent(" ", "Is it?"));
        assertThrows(IllegalArgumentException.class, () -> new DecisionYesNoQuestionContent("q", null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionChoiceQuestionContent("q", "Which?", Map.of("only", "One option")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionChoiceQuestionContent("q", "Which?", Map.of("", "Blank", "other", "Other")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionScoreQuestionContent("q", "How good?", List.of("Bad")));
    }

    @Test
    public void appliesYesNoThreshold() {
        DecisionYesNoAnswerContent answer = new DecisionYesNoAnswerContent("destructive", 0.8);
        assertTrue(answer.isYes());
        assertTrue(answer.isYes(0.8));
        assertFalse(answer.isYes(0.9));
        assertThrows(IllegalArgumentException.class, () -> answer.isYes(1.5));
    }

    @Test
    public void keepsAnswerFields() {
        DecisionChoiceAnswerContent choice = new DecisionChoiceAnswerContent(
                "severity", "high", 0.7, Map.of("low", 0.05, "medium", 0.15, "high", 0.8));
        assertEquals("high", choice.getChoice());
        assertEquals(0.8, choice.getProbabilities().get("high"));

        DecisionScoreAnswerContent score =
                new DecisionScoreAnswerContent("quality", 1.6, 0.5, Map.of(2, 0.7, 0, 0.1, 1, 0.2));
        assertEquals(1.6, score.getScore());
        assertEquals(List.of(0, 1, 2), List.copyOf(score.getProbabilities().keySet()));
    }

    @Test
    public void rejectsInvalidAnswers() {
        assertThrows(IllegalArgumentException.class, () -> new DecisionYesNoAnswerContent("q", 1.2));
        assertThrows(IllegalArgumentException.class, () -> new DecisionYesNoAnswerContent("q", Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionChoiceAnswerContent("q", "missing", 0.5, Map.of("a", 0.5, "b", 0.5)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionChoiceAnswerContent("q", "a", -0.1, Map.of("a", 0.5, "b", 0.5)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionChoiceAnswerContent("q", "a", 0.5, Map.of("a", 0.5, "b", 2.0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionScoreAnswerContent("q", 1, 0.5, Map.of(1, 0.5, 2, 0.5)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionScoreAnswerContent("q", 2.5, 0.5, Map.of(0, 0.2, 1, 0.3, 2, 0.5)));
    }
}
