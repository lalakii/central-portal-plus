# CentralPortalPlus
[![Maven Central](https://img.shields.io/maven-central/v/cn.lalaki.central/central.svg?label=Maven%20Central&logo=sonatype)](https://central.sonatype.com/artifact/cn.lalaki.central/central/)
![License: Apache-2.0 (shields.io)](https://img.shields.io/badge/License-Apache--2.0-c02041?logo=apache)

The plugin implements sonatype's [Central Publisher API](https://central.sonatype.com/api-doc) (part of).

It will call maven-publish to generate artifacts and publish them to [sonatype's central portal](https://central.sonatype.com/).

Note: This is a third party plugin.

### Usage

Apply this plugin in gradle:
```kts
plugins {
    id("cn.lalaki.central") version "1.2.6"
}
```

Add configuration:
```kts
val localMavenRepo = uri("path/of/local_repo") // The path is recommended to be set to an empty directory
centralPortalPlus {
    url = localMavenRepo
    
    // Configure user token.
    username = "..."
    password = "..."
    // or load from xml file
    tokenXml = uri("D:\\user_token.xml")
    
    publishingType = PublishingType.USER_MANAGED // or PublishingType.AUTOMATIC
}
```
About **user_token.xml**, save it to a file.

<img src="https://fastly.jsdelivr.net/gh/lalakii/lalakii.github.io@master/tokenXml.jpg" width="420" alt="tokenXml">

Configure maven-publish as before, doc: [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html), or see: [sample/build.gradle.kts](https://github.com/lalakii/central-portal-plus/blob/master/sample/build.gradle.kts)
```kts
publishing {
    repositories {
        maven {
            url = localMavenRepo // Specify the same local repo path in the configuration.
        }
    }
    // ...
}
```
You are now ready to start the task of publishing to the Central Portal.
```console
# windows
.\gradlew publishToCentralPortal

# other
./gradlew publishToCentralPortal
```
If you need to check the status of the deployment. (If no parameter is provided, the default value will be the last deployment Id)
```console
.\gradlew dumpDeployment -PutId="deployment Id"
```
If you want to remove the deployment. (If no parameter is provided, the default value will be the last deployment Id)
```console
.\gradlew deleteDeployment -PutId="deployment Id"
```
Note: deployments that have been successfully published cannot be deleted.

If you need to get the deployment Id, visit: [Maven Central: Publishing](https://central.sonatype.com/publishing/deployments).

### End for now