package cn.lalaki.pub

import org.gradle.api.Incubating
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
    @Incubating
    AUTOMATIC,

    /**
     * USER_MANAGED: Even if the server is validated,
     * you still need to log in to the central portal to confirm the release
     * [Maven Central: Publishing](https://central.sonatype.com/publishing/deployments)
     */
    USER_MANAGED,

    /**
     * SNAPSHOT: https://central.sonatype.org/publish/publish-portal-snapshots/#publishing-via-other-methods
     */
    SNAPSHOT,
}
