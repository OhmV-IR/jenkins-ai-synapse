# Jenkins AI Synapse

## Introduction

Provides a base API for interacting with AI models in Jenkins. Provides access to the core features of modern models including requests, thinking, tools and skills.

## Getting started

Install the plugin, at least one client implementation and likely a end workflow plugin, configure those in Jenkins and you're off!

## Using models in jobs

### Pipeline

The `aiRequest` step sends a request to a model, runs any tools the model calls, and returns the answer. Progress
is written to the build log as it happens, and aborting the build cancels the request.

```groovy
def summary = aiRequest 'Summarize the changes in this build'

def review = aiRequest(
    promptId: 'code-review',              // a base prompt from Manage Jenkins » Base Prompts
    prompt: diff,                         // added to the base prompt
    model: 'anthropic:claude-opus-5-5',   // optional, by default any model that supports the request is used
    tools: ['readFile'],
    skills: ['java-style'],
    returnDetails: true)                  // return a map instead of only the text
echo review.text
echo "Used ${review.usage?.outputTokens} output tokens"
```

With `returnDetails: true` the step returns a map with `text`, `thinking`, `toolCalls`, `finishReason`, `usage`,
`model` and `conversationId`. Other options are `systemPrompt`, `temperature`, `maxOutputTokens` and `quiet`.

### Freestyle

Add the *Send a request to an AI model* build step. All of its fields accept build variables such as `${GIT_COMMIT}`.

### Conversations

Each request made by a build is recorded on that build under *AI Conversations*, and is deleted with the build.
Conversations created by other plugins through `new ModelConversation()` are stored as JSON in
`JENKINS_HOME/conversations`, listed under *Manage Jenkins » Past Conversations*, and deleted after the number of
days set in *Manage Jenkins » System » AI Conversations* (30 by default, 0 keeps them forever).

## Configuration as code

Models, base prompts, skills and conversation retention can be managed with the
[Configuration as Code](https://plugins.jenkins.io/configuration-as-code/) plugin:

```yaml
unclassified:
  aiModels:
    models:
      - anthropic:                 # the symbol of the model configuration from your client plugin
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
  aiConversations:
    retentionDays: 30
```

Settings from earlier versions (`models.xml`, `prompts.xml` and `skills.xml` in `JENKINS_HOME`) are migrated
automatically on startup; the old files are kept with a `.migrated` suffix.

## Writing a client plugin

A client plugin provides a `ModelConfiguration`, a `ModelDataRetriever`, a `ModelClient` and a
`ModelClientConfiguration`. `ModelClientConfiguration` is a `GlobalConfiguration`: annotate the subclass with
`@Extension` and `@Symbol`, give it a no-argument constructor that calls `load()`, and use `@DataBoundSetter`s that
call `save()` for its settings (for example an API key credentials id). Include a `timeoutSeconds` field in its
`config.jelly`; the core uses it to bound each request.

Clients that stream responses can override the `takeStepImpl` overload that takes a `ModelRequestListener` and call
`onTextDelta` as text arrives, so it appears in the build log immediately. Steps run on a separate thread that is
interrupted when the request is cancelled or times out, so clients should use interruptible I/O.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Currently private and ARR, but this is likely subject to change in the future
