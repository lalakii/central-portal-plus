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
            val workDir = project.layout.projectDirectory.asFile.canonicalPath
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
                tasks.register("cleanLocalMavenRepo", BaseCleanLocalMavenRepoTask::class.java) {
                    it.pluginContext = this
                    it.workDir = workDir
                }
            val defaultCleanTask = tasks.findByName("clean")
            defaultCleanTask?.finalizedBy(cleanLocalRepoTask)
            val publishToCentralPortalTask =
                tasks.register("publishToCentralPortal", BasePublishingTask::class.java) {
                    it.dependsOn(
                        cleanLocalRepoTask,
                    )
                    it.pluginContext = this
                    it.workDir = workDir
                    it.description = "Publish your artifacts to sonatype's central portal."
                }
            tasks.register("dumpDeployment", BaseDeploymentsStatusTask::class.java) {
                it.pluginContext = this
                it.workDir = workDir
            }
            tasks.register("deleteDeployment", BaseDeleteDeploymentTask::class.java) {
                it.pluginContext = this
                it.workDir = workDir
            }
            val defaultPublishTask = tasks.findByName("publish")
            defaultPublishTask?.finalizedBy(publishToCentralPortalTask.get())
        }
    }
}
