package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import lombok.Getter;

public class FileContent implements ModelInput, ModelOutput {
    private @Getter final String fileId;
    private @Getter final byte[] fileData;

    public FileContent(String fileId, byte[] fileData) {
        this.fileId = fileId;
        this.fileData = fileData;
    }
}
