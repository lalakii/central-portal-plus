package cn.lalaki.pub

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
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
    override var url: URI? = null
    override var username: String? = null
    override var password: String? = null
    override var cookies: String? = null
    override var tokenXml: URI? = null
    override var publishingType: PublishingType? = null
    override var connectTimeoutSeconds: Long = CONNECT_TIMEOUT_DEFAULT
    override var readTimeoutSeconds: Long = READ_TIMEOUT_DEFAULT
    override var writeTimeoutSeconds: Long = WRITE_TIMEOUT_DEFAULT
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
        target.afterEvaluate { _ ->
            workDir = target.layout.projectDirectory.asFile.canonicalPath
            if (portalConf.url == null) {
                val publishConf =
                    target.extensions.findByType(PublishingExtension::class.java)
                if (publishConf is PublishingExtension) {
                    val localMavenRepo =
                        publishConf.repositories.find { it is MavenArtifactRepository }
                    if (localMavenRepo is MavenArtifactRepository) {
                        this.url = localMavenRepo.url
                    }
                }
            } else {
                this.url = portalConf.url
            }
            loadUserConfig(portalConf)
            val tasks = target.tasks
            val cleanLocalRepoTask =
                tasks.register("cleanLocalMavenRepo", BaseCleanLocalMavenRepoTask::class.java) {
                    it.pluginContext = this
                }
            val defaultCleanTask = tasks.findByName("clean")
            defaultCleanTask?.finalizedBy(cleanLocalRepoTask.get())
            tasks.register("dumpDeployment", BaseDeploymentsStatusTask::class.java) {
                it.pluginContext = this
            }
            tasks.register("deleteDeployment", BaseDeleteDeploymentTask::class.java) {
                it.pluginContext = this
            }
            val defaultPublishTask = tasks.findByName("publish")
            if (defaultPublishTask != null) {
                defaultPublishTask.dependsOn(cleanLocalRepoTask.get())
                val publishToCentralPortalTask = tasks.register(
                    "publishToCentralPortal",
                    BasePublishingTask::class.java
                ) {
                    it.dependsOn(
                        defaultPublishTask,
                    )
                    it.pluginContext = this
                }
                defaultPublishTask.finalizedBy(publishToCentralPortalTask.get())
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
        val connectTimeout = portalConf.connectTimeoutSeconds
        if (connectTimeout != 0L) {
            this.connectTimeoutSeconds = connectTimeout
        }
        val readTimeout = portalConf.readTimeoutSeconds
        if (readTimeout != 0L) {
            this.readTimeoutSeconds = readTimeout
        }
        val writeTimeout = portalConf.writeTimeoutSeconds
        if (writeTimeout != 0L) {
            this.writeTimeoutSeconds = writeTimeout
        }
    }

    companion object {
        const val READ_TIMEOUT_DEFAULT = 120L
        const val WRITE_TIMEOUT_DEFAULT = 120L
        const val CONNECT_TIMEOUT_DEFAULT = 30L
    }
}
