package cn.lalaki

import cn.lalaki.pub.AbstractTask
import cn.lalaki.pub.MiniCentralExtension
import cn.lalaki.pub.MiniCleanupTask
import cn.lalaki.pub.MiniDeleteTask
import cn.lalaki.pub.MiniDumpTask
import cn.lalaki.pub.MiniPublisherTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * Created on 2026-09-22
 *
 * @author lalakii (i@lalaki.cn)
 * @since Classes used for dependency configuration, and passing parameters.
 */
@Suppress("unused")
open class MiniCentral : Plugin<Project>, Action<Project> {
    @Suppress("NewApi")
    override fun apply(project: Project) {
        project.run {
            arrayOf("signing", "maven-publish").forEach(pluginManager::apply)
            val ext = extensions.create("centralPortalPlus", MiniCentralExtension::class.java)
            MiniCentral::class.java.getResourceAsStream("/META-INF/lang.txt")?.use {
                Properties().apply {
                    load(BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)))
                    ext.languages.set(this)
                }
            }
            val publisherTask = tasks.register(
                "publishToCentralPortal", MiniPublisherTask::class.java, ext
            )
            val cleanupTask = tasks.register(
                "miniCentralCleanup", MiniCleanupTask::class.java, ext
            ).apply {
                configure {
                    it.onlyIf { ext.autoClean.get() && !ext.disabled.get() }
                }
            }
            tasks.named("publish").apply {
                configure {
                    it.finalizedBy(publisherTask)
                    it.dependsOn(cleanupTask)
                }
                publisherTask.configure {
                    it.dependsOn(this)
                    it.onlyIf { !ext.disabled.get() }
                }
            }
            tasks.named("clean").configure {
                it.finalizedBy(cleanupTask)
            }
            tasks.register(
                "dumpDeployment", MiniDumpTask::class.java, ext
            )
            tasks.register(
                "deleteDeployment", MiniDeleteTask::class.java, ext
            )
            afterEvaluate(this@MiniCentral)
        }
    }

    override fun execute(after: Project) {
        after.run {
            val ext = extensions.getByType(MiniCentralExtension::class.java)
            ext.connectTimeoutSeconds.orNull?.takeIf { it > 0 }?.let {
                AbstractTask.HTTP_CONNECT_TIMEOUT_SEC = it
            }
            ext.readTimeoutSeconds.orNull?.takeIf { it > 0 }?.let {
                AbstractTask.HTTP_READ_TIMEOUT_SEC = it
            }
            ext.writeTimeoutSeconds.orNull?.takeIf { it > 0 }?.let {
                AbstractTask.HTTP_WRITE_TIMEOUT_SEC = it
            }
            maybe("UtId").let {
                for (item in it) {
                    if (item != null && hasProperty(item)) {
                        ext.args.set(property(item).toString().trim())
                        break
                    }
                }
            }
            ext.version.set(version.toString())
            if (ext.useGpgCmd.get()) {
                extensions.findByType(SigningExtension::class.java)?.useGpgCmd()
            }
            extensions.findByType(PublishingExtension::class.java)?.apply {
                repositories.filterIsInstance<MavenArtifactRepository>().map { it.url }
                    .apply(ext.repository::set)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun maybe(word: String): Array<String?> {
        var arrayIndex = 0
        val chars = word.toCharArray()
        val ret = arrayOfNulls<String>(pow(2, chars.size))
        fun backtrack(index: Int) {
            if (index == chars.size) {
                ret[arrayIndex] = String(chars)
                arrayIndex++
                return
            }
            val original = chars[index]
            chars[index] = original.lowercaseChar()
            backtrack(index + 1)
            chars[index] = original.uppercaseChar()
            backtrack(index + 1)
        }
        backtrack(0)
        return ret
    }

    @Suppress("SameParameterValue")
    private fun pow(base: Int, exponent: Int): Int {
        var ret = 1
        repeat(exponent) {
            ret *= base
        }
        return ret
    }
}
