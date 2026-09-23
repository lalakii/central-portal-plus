@file:Suppress("UnstableApiUsage")

import cn.lalaki.pub.PublishingType

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.central.portal.plus)
}

/** The sample configuration is as follows **/
android {
    namespace = "icu.lalaki.sample"
    defaultConfig {
        compileSdkPreview = "CinnamonBun"
    }
    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}
group = "icu.lalaki.example"
version = "3.0.0"
centralPortalPlus {
    // username = System.getenv("YOUR_USER")
    // password = System.getenv("YOUR_PASS")

    tokenXml = uri("D:\\user_token.xml")

    // cookies = System.getenv("YOUR_COOKIES")

    /** import cn.lalaki.pub.PublishingType
    1. PublishingType.AUTOMATIC
    2. PublishingType.USER_MANAGED (default)
    3. PublishingType.SNAPSHOT
     */
    publishingType = PublishingType.USER_MANAGED // (optional)

    // network timeout (optional)
    connectTimeoutSeconds = 15
    readTimeoutSeconds = 60
    writeTimeoutSeconds = 60

    // auto clean local build (optional)
    autoClean = true

    // quiet, hide some logs (optional)
    quiet = false
}
publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name = "Sample library"
                artifactId = "Samplelibrary"
                description = "A concise description of my sample"
                url = "http://www.example.com/sample"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "lalaki"
                        name = "lalaki"
                        email = "sample@example.com"
                        roles = listOf("developer")
                        timezone = "Asia/Chongqing"
                        organization = "lalaki"
                        organizationUrl = "https://lalaki.cn"
                    }
                }
                scm {
                    connection = "scm:git:git://example.com/sample.git"
                    developerConnection = "scm:git:ssh://example.com/sample.git"
                    url = "http://example.com/sample/"
                }
                issueManagement {
                    system = "Github"
                    url = "https://github.com/lalakii/central-portal-plus/issues"
                }
            }
        }
    }
}
