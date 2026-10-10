# Jenkins AI Synapse

## Introduction

Provides a base API for interacting with AI models in Jenkins. Provides access to the core features of modern models including requests, thinking, tools and skills.

## Getting started

Install the plugin, at least one client implementation and likely a end workflow plugin, configure those in Jenkins and you're off!

## Skills

Skills don't need any support from the model provider. When a request with skills attached runs, the plugin lists each
skill's name and description in the system prompt and attaches the built-in `read_skill_file` tool, which the model
calls to load a skill's `SKILL.md` and any files it references. Any model with the `TOOLS` and
`ADJUSTABLE_SYSTEM_PROMPT` capabilities can use skills.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Currently private and ARR, but this is likely subject to change in the future
