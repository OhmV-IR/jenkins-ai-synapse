package io.ohmvir.plugins.jenkinscr.api.input;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

public class UniqueModelRequest extends ModelRequest implements Describable<UniqueModelRequest>, ExtensionPoint {
    private @Getter @Setter String requestId = "";
    public UniqueModelRequest(){

    }

    public static class DescriptorImpl extends Descriptor<UniqueModelRequest> {
        @Override
        public @NonNull String getDisplayName() {
            return "Unique Model Request";
        }
    }
}
