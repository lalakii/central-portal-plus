@file:Suppress("UnstableApiUsage")

import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.central.portal.plus)
    `maven-publish`
    signing
}
android {
    namespace = "cn.lalaki.sample"
    defaultConfig {
        compileSdk = 36
    }
}
/** The sample configuration is as follows **/
group = "cn.lalaki.example"
version = "1.0.3"
centralPortalPlus {
    // username = System.getenv("TEMP_USER")
    // password = System.getenv("TEMP_PASS")
    tokenXml = uri("D:\\user_token.xml")
    publishingType = PublishingType.USER_MANAGED
}
signing {
    useGpgCmd()
    sign(publishing.publications)
}
publishing {
    repositories {
        maven {
            url = uri("D:\\repo\\")
        }
    }
    publications {
        create<MavenPublication>("sample") {
            afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
            pom {
                name = "Sample library"
                artifactId = "cn.lalaki.sample"
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
                        name = "lalakii"
                        email = "sample@example.com"
                    }
                }
                scm {
                    connection = "scm:git:git://example.com/sample.git"
                    developerConnection = "scm:git:ssh://example.com/sample.git"
                    url = "http://example.com/sample/"
                }
            }
        }
    }
}
