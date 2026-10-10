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
- `returnResponse: true` returns a map with `text`, `allText`, `thinking`, `model`, `steps`, `finishReason`, `usage`,
  `toolCalls`, `citations`, `files` and `conversationId`.
- `conversationId: "pr-${env.CHANGE_ID}"` continues a conversation across builds of the job.
- `outputDir` saves the images, audio, video and files the model produces to the workspace.
- `topP`, `topK`, `stopSequences`, `outputTypes`, `maxTurns`, `expandEnvVars`, `quiet` and `verbose` are also
  available. The Snippet Generator and the step's help describe each of them.

While the model works, its answer and the tools it calls are printed to the build log, and aborting the build cancels
the request.

## Java API

Requests are immutable and model the conversation explicitly, as messages with a role (`USER`, `ASSISTANT` or `TOOL`)
and content parts (text, media, thinking, tool calls, tool results and citations):

```java
ModelRequest request = ModelRequest.builder()
        .systemPrompt("You are a strict code reviewer")
        .user("Review this change")
        .tool(ToolSpec.of(ToolRegistry.getTool("readFile")))
        .params(GenerationParams.builder().temperature(0.2).build())
        .build();
ModelResponse response = AgentLoop.builder().maxSteps(10).build()
        .run(ModelData.resolve(null, request), request);
String answer = response.getText();
```

`AgentLoop` owns the transcript: it adds each assistant message, runs the tools that message calls and adds their
results, and calls the model again until it stops for a reason other than `TOOL_CALLS`. Failures are reported as
`NoCapableModelException`, `ModelInvocationException` (and `ModelTimeoutException`), `MaxStepsExceededException` or
`MissingOutputException`. Continue a conversation by passing `response.getTranscript()` as the messages of the next
request.

### Writing a client

A client calls one provider. It is a singleton extension shared by every request, so it must be stateless: implement
`StepResult step(ModelContext<C, CC> context, List<Message> transcript)`, translating the transcript, and the system
prompt, settings, tools and skills from `context.getRequest()`, into one API call. Return the assistant message with
the provider's finish reason and token usage, send thinking parts back with their signatures, and report streamed text
to `context.getListener()`.
