> _This repository is maintained by the Develocity Solutions team, as one of several publicly available repositories:_
> - _[Develocity Build Configuration Samples][dv-build-config-samples]_
> - _[Develocity Build Validation Scripts][dv-build-validation-scripts]_
> - _[Develocity Open Source Projects][dv-oss-projects]_
> - _[Common Custom User Data Maven Extension][ccud-maven-extension]_
> - _[Common Custom User Data Gradle Plugin][ccud-gradle-plugin]_
> - _[Common Custom User Data SBT Plugin][ccud-sbt-plugin]_ (this repository)
> - _[Android Cache Fix Gradle Plugin][android-cache-fix-plugin]_

# sbt Develocity Common Custom User Data plugin


[![Verify Build](https://github.com/gradle/common-custom-user-data-sbt-plugin/actions/workflows/build-verification.yml/badge.svg?branch=main)](https://github.com/gradle/common-custom-user-data-sbt-plugin/actions/workflows/build-verification.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.gradle/sbt-develocity-common-custom-user-data)](https://search.maven.org/artifact/com.gradle/sbt-develocity-common-custom-user-data)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.solutions-team.gradle.com/scans)

The sbt Develocity Common Custom User Data plugin enhances published build scans
by adding a set of tags, links and custom values that have proven to be useful for many projects building with Develocity.

You can leverage this plugin for your project in one of two ways:
1. [Apply the published plugin](#applying-the-published-plugin) directly in your build and immediately benefit from enhanced build scans
2. Copy this repository and [develop a customized version of the plugin](#developing-a-customized-version-of-the-plugin) to standardize Develocity usage across multiple projects

## Applying the published plugin

The sbt Develocity Common Custom User Data plugin is available in the [Maven Central](https://search.maven.org/artifact/com.gradle/sbt-develocity-common-custom-user-data). This plugin
requires the [sbt Develocity plugin](https://search.maven.org/artifact/com.gradle/sbt-develocity) to also be applied in your build in order to have
an effect.

In order for the sbt Develocity Common Custom User Data plugin to become active, you need to add it in the `project/plugins.sbt` file of your project. The `project/plugins.sbt` file is the same
file where you have already declared the sbt Develocity plugin.

See [here](./project/plugins.sbt) for an example.

### Version compatibility

This table details the version compatibility of the sbt Develocity Common Custom User Data plugin with the sbt Develocity plugin.

| sbt Develocity Common Custom User Data plugin (this) versions | sbt Develocity plugin versions | sbt versions |
|---------------------------------------------------------------|--------------------------------|--------------|
| 1.0                                                           | 1.0                            | 1.6.0+       |

## Captured data

The additional tags, links and custom values captured by this plugin include:
- A tag representing the operating system
- A tag representing how the build was invoked, be that from your IDE (IDEA, Eclipse) or from the command-line
- A tag representing builds run on CI, together with a set of tags, links and custom values specific to the CI server running the build
- For Git repositories, information on the commit id, branch name, status, and whether the checkout is dirty or not

See [CustomBuildScanEnhancements.scala](./src/main/scala/com/gradle/internal/CustomBuildScanEnhancements.scala) for details on what data is
captured and under which conditions.

## Configuration overrides

This plugin also allows overriding various Develocity related settings via system properties and environment variables:
- Develocity general configuration

See [Overrides.scala](./src/main/scala/com/gradle/internal/Overrides.scala) for the override behavior.

You can use the system properties and environment variables to override Develocity related settings temporarily without having
to modify the build scripts. For example, to change the Develocity server when running a build:

```bash
sbt -Ddevelocity.url=https://ge.solutions-team.gradle.com/ run
```

<details>
  <summary>Click to see the complete set of available system properties and environment variables in the table below. </summary>

### Develocity settings

| Develocity API                  | System property                 | Environment variable            |
|:--------------------------------|:--------------------------------|:--------------------------------|
| develocity.server               | develocity.url                  | DEVELOCITY_URL                  |
| develocity.allowUntrustedServer | develocity.allowUntrustedServer | DEVELOCITY_ALLOWUNTRUSTEDSERVER |


## Developing a customized version of the plugin

For more flexibility, we recommend creating a copy of this repository so that you may develop a customized version of the plugin and publish it internally for your projects to consume.

This approach has a number of benefits:
- Tailor the build scan enhancements to exactly the set of tags, links and custom values you require
- Standardize the configuration for connecting to Develocity and the remote build cache in your organization, removing the need for each project to specify this configuration

If your customized plugin provides all required Develocity configuration, then a consumer project will get all the benefits of Develocity simply by applying the plugin. The
project sources provide a good template to get started with your own plugin.

Refer to the [Javadoc](https://docs.gradle.com/enterprise/sbt-plugin/api/com/gradle/develocity/agent/sbt/api/configuration/) for more details on the key types available for use.

## Changelog

Refer to the [release history](https://github.com/gradle/common-custom-user-data-sbt-plugin-plugin/releases) to see detailed changes on the versions.

## Learn more

Visit our website to learn more about [Develocity][develocity].

## License

The Gradle Enterprise Common Custom User Data Gradle plugin is open-source software released under the [Apache 2.0 License][apache-license].

[dv-build-config-samples]: https://github.com/gradle/develocity-build-config-samples
[dv-build-validation-scripts]: https://github.com/gradle/gradle-enterprise-build-validation-scripts
[dv-oss-projects]: https://github.com/gradle/develocity-oss-projects
[ccud-gradle-plugin]: https://github.com/gradle/common-custom-user-data-gradle-plugin
[ccud-maven-extension]: https://github.com/gradle/common-custom-user-data-maven-extension
[ccud-sbt-plugin]: https://github.com/gradle/common-custom-user-data-sbt-plugin-plugin/
[android-cache-fix-plugin]: https://github.com/gradle/android-cache-fix-gradle-plugin
[develocity]: https://gradle.com/develocity
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0.html
