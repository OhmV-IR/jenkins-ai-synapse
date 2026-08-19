package io.ohmvir.plugins.jenkinscr.api;

import java.util.List;

public class ModelData {
    // TODO
    public static ModelData getFromConfiguration(){
        return null;
    }

    public List<ModelCapability> capabilities;
    public boolean hasCapability(ModelCapability capability){
        return capabilities.contains(capability);
    }

    private ModelProviderType providerType;
    public ModelProviderType getProviderType() {
        return providerType;
    }
}
