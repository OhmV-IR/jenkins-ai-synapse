# jenkinscr

## Introduction

Provides a base API for interacting with AI models in Jenkins. Provides access to the core features of modern models including requests, thinking, tools and skills.

## Getting started

Install the plugin, at least one client implementation and likely a end workflow plugin, configure those in Jenkins and you're off!

## Using models from Pipeline

The `aiRequest` step sends a request to a configured model. In its simplest form it takes the prompt and returns the
answer as a string:

```groovy
def summary = aiRequest 'Summarize the release notes in three bullet points'
```

Every other parameter is optional, so you only add what you need:

```groovy
node {
    sh 'git diff origin/main > changes.diff'
    def review = aiRequest(
        prompt: 'Review this change and list any bugs you find',
        model: 'anthropic:claude-sonnet',  // omit to use the first configured model that supports the request
        systemPrompt: 'You are a strict code reviewer',
        files: ['changes.diff'],           // workspace files to attach: text, documents, images, audio or video
        tools: ['readFile'],               // tools the model may call
        skills: ['code-review'],           // skills to give the model
        temperature: 0.2,
        maxOutputTokens: 4000,
        thinkingLevel: 'high',
        timeout: 300,                      // seconds for the whole request
        returnResponse: true               // return a map instead of the answer only
    )
    echo review.text
    echo "Used ${review.usage?.outputTokens} output tokens over ${review.toolCalls.size()} tool calls"
}
```

- `promptId` starts from a prompt stored under *Manage Jenkins » Base Prompts*; a `prompt` given alongside it is
  appended, and settings given to the step override the stored ones.
- `returnResponse: true` returns a map with `text`, `allText`, `thinking`, `model`, `finishReason`, `usage`,
  `toolCalls`, `citations`, `files` and `conversationId`.
- `conversationId: "pr-${env.CHANGE_ID}"` continues a conversation across builds of the job.
- `outputDir` saves the images, audio, video and files the model produces to the workspace.
- `topP`, `topK`, `stopSequences`, `outputTypes`, `maxTurns`, `expandEnvVars`, `quiet` and `verbose` are also
  available. The Snippet Generator and the step's help describe each of them.

While the model works, its answer and the tools it calls are printed to the build log, and aborting the build cancels
the request.

Plugins can do the same through the Java API: build a `ModelRequest`, then call `executeOrThrow` with a model from
`ModelData`. Attach a `ModelRequestListener` with `setListener` to follow the request's progress.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Currently private and ARR, but this is likely subject to change in the future
