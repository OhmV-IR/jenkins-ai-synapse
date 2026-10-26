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
calls to load a skill's `SKILL.md` and any files it references. Any model with the `TOOLS` and
`ADJUSTABLE_SYSTEM_PROMPT` capabilities can use skills.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under the [GNU Lesser General Public License v3.0](COPYING.LESSER), which supplements the [GNU General Public License v3.0](COPYING).
