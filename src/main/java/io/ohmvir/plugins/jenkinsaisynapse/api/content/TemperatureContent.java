package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import lombok.Getter;

public class TemperatureContent implements ModelInput {
    private @Getter final double temperature;

    public TemperatureContent(double temperature) {
        this.temperature = temperature;
    }
}
