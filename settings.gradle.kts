pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}
plugins {
    id("com.gradle.develocity") version "3.19.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        publishing.onlyIf { false }
    }
}
rootProject.name = "CentralPortalPlus"
includeBuild("central")
include(":sample")
