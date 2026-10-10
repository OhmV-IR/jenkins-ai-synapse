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
  <groupId>io.ohmvir.plugins</groupId>
  <artifactId>jenkinsaisynapse</artifactId>
  <version>VERSION</version>
</dependency>
```

Snapshot builds of `master` are published to `https://central.sonatype.com/repository/maven-snapshots/`.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Currently private and ARR, but this is likely subject to change in the future
