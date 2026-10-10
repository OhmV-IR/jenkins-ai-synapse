# Jenkins AI Synapse

## Introduction

Provides a base API for interacting with AI models in Jenkins. Provides access to the core features of modern models including requests, thinking, tools and skills.

## Getting started

Install the plugin, at least one client implementation and likely a end workflow plugin, configure those in Jenkins and you're off!

## Writing a provider

A provider plugin implements one extension point, `ModelProvider`, for its own `ModelConfiguration` subclass:

```java
@Extension
public class MyProvider extends ModelProvider<MyModelConfiguration> {
    public MyProvider() {
        super(MyModelConfiguration.class);
    }

    @Override
    protected ModelData describe(MyModelConfiguration config) {
        ModelData data = new ModelData(config);
        data.getCapabilities().add(ModelCapability.TOOLS);
        data.getCapabilities().add(ModelCapability.of("myvendor:computer-use"));
        data.getInputs().add(ModelInputType.TEXT);
        data.getOutputs().add(ModelOutputType.UNSTRUCTURED_TEXT);
        return data;
    }

    @Override
    protected ModelClient createClient(MyModelConfiguration config) {
        return (modelData, request, turnInputs) -> callMyApi(config, request, turnInputs);
    }
}
```

Capabilities, input types and output types the core plugin doesn't define can be added with a namespaced id such as
`ModelCapability.of("myvendor:computer-use")` or `ModelInputType.of("myvendor:pdf")`.

## Choosing a model

A request runs on the model named with `ModelRequest.setModelId(...)` (or the `modelId` option of the build step). If
no model is named, it runs on the first model that can handle it, in the order the models are listed on the Models
page. A requested thinking level is mapped to the nearest level the model supports.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Currently private and ARR, but this is likely subject to change in the future
