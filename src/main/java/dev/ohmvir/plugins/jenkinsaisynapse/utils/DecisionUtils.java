package dev.ohmvir.plugins.jenkinsaisynapse.utils;

import java.util.Map;

public class DecisionUtils {
    /**
     * Validates the name a decision question is asked and answered under.
     * @param name The question name
     * @return The validated name
     * @throws IllegalArgumentException If the name is missing or blank
     */
    public static String requireQuestionName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Decision question name is required");
        }
        return name;
    }

    /**
     * Validates that a value is a probability or confidence, ie a finite number from 0 to 1.
     * @param value The value to validate
     * @param field The name of the field the value is for, used in the error message
     * @return The validated value
     * @throws IllegalArgumentException If the value is not finite or is outside 0 to 1
     */
    public static double requireProbability(double value, String field) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " must be a probability from 0 to 1, got: " + value);
        }
        return value;
    }

    /**
     * Validates every probability in a distribution over a decision's options or levels.
     * @param probabilities The distribution to validate
     * @param field The name of the field the distribution is for, used in the error message
     * @return The validated distribution
     * @throws IllegalArgumentException If the distribution is missing, empty or has a value that is not a probability
     */
    public static <K> Map<K, Double> requireProbabilities(Map<K, Double> probabilities, String field) {
        if (probabilities == null || probabilities.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        probabilities.forEach((key, probability) -> {
            if (probability == null) {
                throw new IllegalArgumentException(field + " has no probability for " + key);
            }
            requireProbability(probability, field + "[" + key + "]");
        });
        return probabilities;
    }
}
