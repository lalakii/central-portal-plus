@file:Suppress("UnstableApiUsage")

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

val javaVersion = JavaVersion.VERSION_17.majorVersion
val projectUrl = "https://github.com/lalakii/central-portal-plus"
val central: PluginDependency =
    libs.plugins.central.portal.plus
        .get()
group = central.pluginId
version = central.version
java.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
plugins {
    alias(libs.plugins.kotlin.jvm)
    signing
    alias(libs.plugins.central.portal.plus) version libs.versions.central.portal.plus.last
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.detekt)
}
kotlin {
    jvmToolchain(javaVersion.toInt())
}
detekt {
    toolVersion =
        libs.plugins.detekt
            .get()
            .version
            .toString()
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
tasks.withType<Detekt>().configureEach {
    jvmTarget = javaVersion
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = javaVersion
}
signing {
    useGpgCmd()
    sign(publishing.publications)
}
gradlePlugin {
    website = projectUrl
    vcsUrl = projectUrl
    plugins.create("CentralPortalPlus") {
        id = group.toString()
        displayName = "CentralPortalPlus"
        description = "Publish your artifacts to sonatype's central portal."
        tags = listOf("maven", "maven-central", "publisher", "sonatype", "gradle-plugin")
        implementationClass = "cn.lalaki.pub.CentralPortalPlusPlugin"
    }
}
afterEvaluate {
    tasks.withType(GenerateMavenPom::class.java) {
        if (this.name.startsWith("generatePomFile", ignoreCase = true)
        ) {
            doFirst {
                pom.apply {
                    url = projectUrl
                    description = "Publish your artifacts to sonatype's central portal."
                    licenses {
                        license {
                            name = "Apache-2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            name = "lalakii"
                            email = "i@lalaki.cn"
                        }
                    }
                    organization {
                        name = "lalakii"
                        url = "https://lalaki.cn"
                    }
                    scm {
                        connection = "scm:git:$projectUrl"
                        developerConnection = "scm:git:$projectUrl"
                        url = projectUrl
                    }
                }
            }
        }
    }
}
centralPortalPlus {
    username = System.getenv("TEMP_USER")
    password = System.getenv("TEMP_PASS")
}
publishing {
    repositories {
        maven {
            url = uri("./repo/")
        }
    }
    publications {
        create<MavenPublication>("CentralPortalPlus") {
            this.pom.withXml {
                val node = asNode().appendNode("dependencies")
                project.configurations.runtimeClasspath
                    .get()
                    .resolvedConfiguration.firstLevelModuleDependencies
                    .forEach {
                        val dn = node.appendNode("dependency")
                        dn.appendNode("groupId", it.moduleGroup)
                        dn.appendNode("artifactId", it.moduleName)
                        dn.appendNode("version", it.moduleVersion)
                        dn.appendNode("scope", "runtime")
                    }
            }
        }
    }
}
dependencies {
    implementation(libs.gson)
    implementation(libs.zip4j)
    implementation(libs.okhttp)
    implementation(libs.commons.io)
    implementation(libs.stdlib.jdk8)
}
