package io.ohmvir.plugins.jenkinscr.api.models.retrievers;

import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.api.models.ModelDataRetriever;
import io.ohmvir.plugins.jenkinscr.api.models.ModelProviderType;
import io.ohmvir.plugins.jenkinscr.configuration.models.OllamaModelConfiguration;

public class OllamaModelDataRetriever extends ModelDataRetriever<OllamaModelConfiguration>
{
    public OllamaModelDataRetriever() {
        super(OllamaModelConfiguration.class);
    }

    @Override
    public ModelData retrieveFromConfiguration(OllamaModelConfiguration configuration) {
        return null;
    }
}
