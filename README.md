# Jenkins AI Synapse

## Introduction

Provides a base API for interacting with AI models in Jenkins. Provides access to the core features of modern models including requests, thinking, tools and skills.

## Getting started

Install the plugin, at least one client implementation and likely a end workflow plugin, configure those in Jenkins and you're off!

The plugin requires Jenkins to run on Java 25 or newer.

### Using the API from another plugin

Releases are published to Maven Central:

```xml
<dependency>
  <groupId>dev.ohmvir.plugins</groupId>
  <artifactId>jenkinsaisynapse</artifactId>
  <version>VERSION</version>
</dependency>
```

Snapshot builds of `master` are published to `https://central.sonatype.com/repository/maven-snapshots/`.

## Skills

Skills don't need any support from the model provider. When a request with skills attached runs, the plugin lists each
skill's name and description in the system prompt and attaches the built-in `read_skill_file` tool, which the model
calls to load its `SKILL.md` and any files it references. Any model with the `TOOLS` and
`ADJUSTABLE_SYSTEM_PROMPT` capabilities can use skills.

## Using models in jobs

### Pipeline

The `aiRequest` step sends a request to a model, runs any tools the model calls, and returns the answer. Progress
is written to the build log as it happens, and aborting the build cancels the request.

```groovy
def summary = aiRequest 'Summarize the changes in this build'

def review = aiRequest(
    promptId: 'code-review',
    prompt: diff,
    model: 'anthropic:claude-opus-5-5',
    tools: ['readFile'],
    skills: ['java-style'],
    returnDetails: true)
echo review.text
echo "Used ${review.usage?.outputTokens} output tokens"
```

With `returnDetails: true` the step returns a map with `text`, `thinking`, `toolCalls`, `finishReason`, `usage`,
`model` and `conversationId`. Other options are `systemPrompt`, `temperature`, `maxOutputTokens` and `quiet`.

### Freestyle

Add the *Send a request to an AI model* build step. All of its fields accept build variables such as `${GIT_COMMIT}`.

Each request made by a build is recorded on that build under *AI Conversations*, and is deleted with the build.

## Configuration as code

Models, base prompts and skills can be managed with the
[Configuration as Code](https://plugins.jenkins.io/configuration-as-code/) plugin:

```yaml
unclassified:
  aiModels:
    models:
      - anthropic:
          modelName: "claude-opus-5-5"
          modelDisplayName: "Claude Opus"
  aiPrompts:
    prompts:
      - simplePrompt:
          promptId: "summarize"
          promptText: "Summarize the build"
  aiSkills:
    skills:
      - basicSkill:
          skillName: "reviewer"
          skillDescription: "Reviews code"
          skillText: "Review the code carefully"
```

Settings from earlier versions (`models.xml`, `prompts.xml` and `skills.xml` in `JENKINS_HOME`) are migrated
automatically on startup; the old files are kept with a `.migrated` suffix.

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

Licensed under the [GNU Lesser General Public License v3.0](COPYING.LESSER), which supplements the
[GNU General Public License v3.0](COPYING).
