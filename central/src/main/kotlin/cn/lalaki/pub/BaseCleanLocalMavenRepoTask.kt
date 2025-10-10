package cn.lalaki.pub

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Created on 2024-06-21
 *
 * @author lalakii (i@lalaki.cn)
 * @since This class is used to clean the local maven repository.
 */
@Suppress("NewApi")
abstract class BaseCleanLocalMavenRepoTask : AbstractTask() {
    /**
     * clean local maven repo
     */
    @TaskAction
    fun launch() {
        val url = pluginContext.url ?: return
        val localMaven = Paths.get(url).toFile()
        if (localMaven.parent == null) {
            logger.error(
                "It is not allowed to use the root directory ({}) as a local maven repo!",
                localMaven,
            )
        } else if (validate(localMaven)) {
            logger.error(
                "It is not allowed to use a folder ({}) that exists in the environment variable as a local maven repo!",
                localMaven,
            )
        } else {
            clean(localMaven)
        }
    }

    private fun validate(localMaven: File): Boolean {
        for (item in System.getenv()) {
            try {
                val protectDir = File(item.value)
                val isDir = FileUtils.isDirectory(protectDir)
                if (isDir && protectDir == localMaven) {
                    return true
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    private fun clean(localMaven: File) {
        val isDir = localMaven.isDirectory
        if (isDir) {
            if (FileUtils.isEmptyDirectory(localMaven)) return
            FileUtils.cleanDirectory(localMaven)
        } else if (localMaven.exists()) {
            FileUtils.copyFile(
                localMaven,
                File(localMaven.parent, localMaven.name + ".bak"),
                StandardCopyOption.REPLACE_EXISTING,
            )
            FileUtils.forceDelete(localMaven)
        }
        if (!isDir) {
            try {
                FileUtils.forceMkdir(localMaven)
            } catch (e: IOException) {
                logger.error(e.localizedMessage)
            }
        }
    }
}
