package cn.lalaki.pub

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import java.net.URI

/**
 * Created on 2024-06-21
 *
 * @author lalakii (i@lalaki.cn)
 * @since Classes used for dependency configuration, and passing parameters.
 */
@Suppress("unused")
class CentralPortalPlusPlugin :
    BaseCentralPortalPlusExtension(),
    Plugin<Project> {
    override lateinit var url: Property<URI>
    override lateinit var username: Property<String>
    override lateinit var password: Property<String>
    override lateinit var cookies: Property<String>
    override lateinit var tokenXml: Property<URI>
    override lateinit var publishingType: Property<PublishingType>
    override lateinit var connectTimeoutSeconds: Property<Long>
    override lateinit var readTimeoutSeconds: Property<Long>
    override lateinit var writeTimeoutSeconds: Property<Long>
    lateinit var workDir: String

    override fun apply(target: Project) {
        val pluginManager = target.pluginManager
        pluginManager.apply("signing")
        pluginManager.apply("maven-publish")
        val portalConf =
            target.extensions.create(
                "centralPortalPlus",
                BaseCentralPortalPlusExtension::class.java,
            )
        this.url = portalConf.url
        target.afterEvaluate { _ ->
            workDir = target.layout.projectDirectory.asFile.canonicalPath
            if (!portalConf.url.isPresent) {
                val publishConf =
                    target.extensions.findByType(PublishingExtension::class.java)
                if (publishConf is PublishingExtension) {
                    val localMavenRepo =
                        publishConf.repositories.find { it is MavenArtifactRepository }
                    if (localMavenRepo is MavenArtifactRepository) {
                        this.url.set(localMavenRepo.url)
                    }
                }
            }
            loadUserConfig(portalConf)
            val tasks = target.tasks
            val cleanLocalRepoTask =
                tasks.register("cleanLocalMavenRepo", BaseCleanLocalMavenRepoTask::class.java) {
                    it.notCompatibleWithConfigurationCache("notCompatibleWithConfigurationCache")
                    it.pluginContext = this
                }
            val defaultCleanTask = tasks.findByName("clean")
            defaultCleanTask?.finalizedBy(cleanLocalRepoTask.get())
            tasks.register("dumpDeployment", BaseDeploymentsStatusTask::class.java) {
                it.notCompatibleWithConfigurationCache("notCompatibleWithConfigurationCache")
                it.pluginContext = this
            }
            tasks.register("deleteDeployment", BaseDeleteDeploymentTask::class.java) {
                it.notCompatibleWithConfigurationCache("notCompatibleWithConfigurationCache")
                it.pluginContext = this
            }
            val defaultPublishTask = tasks.findByName("publish")
            if (defaultPublishTask != null) {
                defaultPublishTask.dependsOn(cleanLocalRepoTask)
                val publishToCentralPortalTask = tasks.register(
                    "publishToCentralPortal",
                    BasePublishingTask::class.java
                ) {
                    it.notCompatibleWithConfigurationCache("notCompatibleWithConfigurationCache")
                    it.dependsOn(
                        defaultPublishTask,
                        tasks.withType(Jar::class.java),
                        tasks.withType(Javadoc::class.java)
                    )
                    it.pluginContext = this
                }
                defaultPublishTask.finalizedBy(publishToCentralPortalTask)
            } else {
                target.logger.error("missing default publish task!")
            }
        }
    }

    private fun loadUserConfig(portalConf: BaseCentralPortalPlusExtension) {
        this.username = portalConf.username
        this.password = portalConf.password
        this.tokenXml = portalConf.tokenXml
        this.cookies = portalConf.cookies
        this.publishingType = portalConf.publishingType
        this.connectTimeoutSeconds = portalConf.connectTimeoutSeconds
        this.readTimeoutSeconds = portalConf.readTimeoutSeconds
        this.writeTimeoutSeconds = portalConf.writeTimeoutSeconds
    }

}
