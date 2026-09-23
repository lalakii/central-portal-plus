# CentralPortalPlus

[![Maven Central](https://img.shields.io/maven-central/v/cn.lalaki.central/central.svg?label=Maven%20Central&logo=sonatype)](https://central.sonatype.com/artifact/cn.lalaki.central/central/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6ca71f005bd44dc4893761c16007b0ea)](https://app.codacy.com/gh/lalakii/central-portal-plus/dashboard)
![License: Apache-2.0 (shields.io)](https://img.shields.io/badge/License-Apache--2.0-c02041?logo=apache)

[<img src="https://fastly.jsdelivr.net/gh/lalakii/central-portal-plus@master/icon.png" width="64" alt="CentralPortalPlus">](https://central.sonatype.com/artifact/cn.lalaki.central/central)

Language: [English](./README.md) | [简体中文](./README-zh_CN.md)

The plugin implements sonatype's [Central Publisher API](https://central.sonatype.com/api-doc) (part
of).

It will call maven-publish to generate artifacts and publish them
to [sonatype's central portal](https://central.sonatype.com/)

Note: This is a third party plugin.

**Features**

- **SNAPSHOT**: Supports publishing "-SNAPSHOT" versions.
- **Cache**: Supports cache configuration.

## Usage

Apply this plugin in Gradle:

```kts
plugins {
    id("cn.lalaki.central") version "3.0.3"
}
```

Add configuration:

```kts
centralPortalPlus {
    // Configure user token.
    username = "..."
    password = "..."
  
    // or load from XML file
    tokenXml = uri("D:\\user_token.xml")
  
    // or set cookies
    cookies = "..." // Check the HTTPs headers for cookies. -> https://central.sonatype.com

    /** import cn.lalaki.pub.PublishingType
    1. PublishingType.AUTOMATIC
    2. PublishingType.USER_MANAGED
    3. PublishingType.SNAPSHOT
     */
    publishingType = PublishingType.USER_MANAGED
}
```

<p align="center">( About "user_token.xml", save it to a file. )</p>
<p align="center">
<img src="https://fastly.jsdelivr.net/gh/lalakii/lalakii.github.io@master/tokenXml.jpg" width="450" alt="tokenXml">
</p>

Configure maven-publish as before,
doc: [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html), or
see: [sample/build.gradle.kts](https://github.com/lalakii/central-portal-plus/blob/master/sample/build.gradle.kts)

```kts
val localMavenRepo =
  uri("path/of/local_repo") // The path is recommended to be set to an empty directory

publishing {
    repositories {
        maven {
            url = localMavenRepo // Specify the same local repo path in the configuration.
        }
        // or
        mavenLocal()
    }
    // ...
}
```

You are now ready to start the task of publishing to the Central Portal.

```console
REM ---- On Windows ----
.\gradlew publishToCentralPortal
REM ----     or     ----
.\gradlew publish
```
```shell
# Linux / Unix / macOS
./gradlew publishToCentralPortal
#       or
./gradlew publish
```

If you need to check the status of the deployment.

```console
.\gradlew dumpDeployment -PUTID="deployment Id"
```

If you want to remove the deployment.

```console
.\gradlew deleteDeployment -PUTID="deployment Id"
```

Note: deployments that have been successfully published cannot be deleted.

You can track your deployment on the official
website: [Maven Central: Publishing](https://central.sonatype.com/publishing/deployments)

## License
[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)