import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask

val javaVersion: String = JavaVersion.VERSION_17.majorVersion
val projectName = "CentralPortalPlus"
val projectDescription = "Publish your artifacts to sonatype's central portal."
val projectUrl = "https://github.com/lalakii/central-portal-plus"
val central: PluginDependency = libs.plugins.central.portal.plus.get()
group = central.pluginId
version = central.version
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.central.portal.plus) version (libs.versions.central.portal.plus.last)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.detekt)
}
kotlin {
    jvmToolchain(javaVersion.toInt())
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}
detekt {
    toolVersion =
        libs.versions.detekt.asProvider().get()
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}
tasks.withType<Detekt>().configureEach {
    jvmTarget = javaVersion
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = javaVersion
}
publishing {
    repositories {
        mavenLocal()
    }
    afterEvaluate {
        publications.forEach {
            if (it is MavenPublication) {
                it.pom.apply {
                    name = projectName
                    url = projectUrl
                    description = "Publish your artifacts to sonatype's central portal."
                    licenses {
                        license {
                            name = "Apache-2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    issueManagement {
                        system = "Github"
                        url = "${projectUrl}/issues"
                    }
                    developers {
                        developer {
                            id = "lalaki"
                            name = "lalaki"
                            email = "i@lalaki.cn"
                            roles = listOf("developer")
                            timezone = "Asia/Chongqing"
                            organization = "lalaki"
                            organizationUrl = "https://lalaki.cn"
                        }
                    }
                    organization {
                        name = "lalaki"
                        url = "https://lalaki.cn"
                    }
                    scm {
                        connection = "scm:git:${projectUrl}.git"
                        developerConnection = "scm:git:${projectUrl}.git"
                        url = projectUrl
                    }
                }
            }
        }
    }
}
gradlePlugin {
    website = projectUrl
    vcsUrl = projectUrl
    plugins {
        create(projectName) {
            id = group.toString()
            displayName = projectName
            description = projectDescription
            tags = listOf("maven", "maven-central", "publisher", "sonatype", "gradle-plugin")
            implementationClass = "cn.lalaki.MiniCentral"
        }
    }
}
centralPortalPlus {
    tokenXml = uri("D:\\user_token.xml")
}
dependencies {
    compileOnly(gradleApi())
    implementation(libs.gson)
    implementation(libs.commons.compress2)
    implementation(libs.okhttp)
    implementation(libs.commons.io)
    implementation(libs.stdlib.jdk8)
    detektPlugins(libs.detekt.formatting)
}
