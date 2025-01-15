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
    override var publishingType: PublishingType? = null
    override var tokenXml: URI? = null

    override fun apply(target: Project) {
        val portalConf =
            target.extensions.create(
                "centralPortalPlus",
                BaseCentralPortalPlusExtension::class.java,
            )
        target.afterEvaluate { project ->
            if (portalConf.url == null) {
                val publishConf =
                    project.extensions.findByType(PublishingExtension::class.java)
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
            this.username = portalConf.username
            this.password = portalConf.password
            this.tokenXml = portalConf.tokenXml
            this.publishingType = portalConf.publishingType
            val tasks = project.tasks
            val cleanLocalRepoTask =
                tasks.register("cleanLocalMavenRepo", BaseCleanLocalMavenRepoTask::class.java)
            val defaultPublishTask = tasks.findByName("publish")
            val defaultCleanTask = tasks.findByName("clean")
            defaultCleanTask?.finalizedBy(cleanLocalRepoTask)
            if (defaultPublishTask != null) {
                defaultPublishTask.dependsOn(cleanLocalRepoTask)
                val publishToCentralPortalTask =
                    tasks.register("publishToCentralPortal", BasePublishingTask::class.java)
                publishToCentralPortalTask.configure { config ->
                    config.dependsOn(
                        defaultPublishTask,
                    )
                }
            } else {
                target.logger.error("missing default publish task!")
            }
            tasks.register("dumpDeployment", BaseDeploymentsStatusTask::class.java)
            tasks.register("deleteDeployment", BaseDeleteDeploymentTask::class.java)
        }
    }
}
