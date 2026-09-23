package cn.lalaki.pub

import okio.IOException
import org.gradle.internal.os.OperatingSystem
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath

abstract class MiniCleanupTask : AbstractTask {
    @Suppress("unused")
    @Inject
    constructor(e: MiniCentralExtension) : super(e)

    override fun launch() {
        if (ext.repository.isPresent) {
            val protectedPaths = getProtectedPaths()
            ext.url.orNull?.apply {
                ext.repository.add(this)
            }
            ext.repository.get().forEach {
                val path = it.toPath()
                if (path.isDirectory() && !validateProtectedPath(protectedPaths, path)) {
                    deleteDirs(path)
                }
            }
        }
    }

    @Suppress("NewApi")
    private fun validateProtectedPath(protectedPaths: List<String>, dir: Path): Boolean {
        for (sysPath in protectedPaths) {
            if (dir.startsWith(sysPath)) {
                return true
            }
        }
        return false
    }

    @Suppress("NewApi")
    @OptIn(ExperimentalPathApi::class)
    private fun deleteDirs(path: Path) {
        try {
            path.deleteRecursively()
        } catch (e: IOException) {
            if (!silent()) {
                logger.error(
                    "{}{}{}Directory: {}",
                    System.lineSeparator(),
                    e.localizedMessage,
                    System.lineSeparator(),
                    path
                )
            }
        }
    }

    private fun getProtectedPaths(): List<String> {
        return System.getenv("PATH")
            .split(
                if (OperatingSystem.current().isWindows) {
                    ";"
                } else {
                    ":"
                }
            )
    }
}
