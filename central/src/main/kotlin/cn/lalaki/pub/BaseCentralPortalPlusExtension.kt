package cn.lalaki.pub

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.net.URI

/**
 * Created on 2024-06-21
 *
 * @author lalakii (i@lalaki.cn)
 * @since extension class
 */
abstract class BaseCentralPortalPlusExtension {
    @get:Input
    @get:Optional
    abstract val username: Property<String>

    @get:Input
    @get:Optional
    abstract val password: Property<String>

    @get:Input
    @get:Optional
    abstract val tokenXml: Property<URI>

    @get:Input
    @get:Optional
    abstract val cookies: Property<String>

    @get:Input
    @get:Optional
    abstract val publishingType: Property<PublishingType>

    @get:Input
    @get:Optional
    abstract val url: Property<URI>

    @get:Input
    @get:Optional
    abstract val connectTimeoutSeconds: Property<Long>

    @get:Input
    @get:Optional
    abstract val readTimeoutSeconds: Property<Long>

    @get:Input
    @get:Optional
    abstract val writeTimeoutSeconds: Property<Long>

    /***
     * The endpoint has two optional query parameters.
     *
     * The first, name, allows for providing a human-readable name for the deployment.
     * The second, publishingType, can have one of the following values:
     *
     * AUTOMATIC: (default) a deployment will go through validation and, if it passes,
     *   automatically proceed to publish to Maven Central
     *
     * USER_MANAGED: a deployment will go through validation and require the user to
     *   manually publish it via the Portal UI
     */
    @Suppress("unused")
    enum class PublishingType {
        /***
         * AUTOMATIC: Server validation will automatically publish
         */
        AUTOMATIC,

        /**
         * USER_MANAGED: Even if the server is validated,
         * you still need to log in to the central portal to confirm the release
         * [Maven Central: Publishing](https://central.sonatype.com/publishing/deployments)
         */
        USER_MANAGED,
    }
}
