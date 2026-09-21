package cn.lalaki.pub

import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.tasks.TaskAction
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Deflater
import kotlin.io.path.toPath

/**
 * Created on 2024-06-20
 *
 * @author lalaki (i@lalaki.cn)
 * @since Classes for publishing artifacts to the publisher API.
 */
@Suppress("NewApi")
abstract class BasePublishingTask : AbstractTask() {
    private val bundleFileName = "bundle.zip"

    /***
     * Creating bundles and publishing
     */
    @TaskAction
    fun launch() {
        val url = pluginContext.url
        if (!url.isPresent) {
            throw NullPointerException("missing local maven repo, url=" + null)
        }
        val dir = url.get().toPath().toFile()
        if (!dir.isDirectory) {
            logger.error("local maven repo ({}) is not a folder!", dir)
            return
        }
        val groups = dir.listFiles()
        if (groups == null) {
            logger.error("local maven repo ({}) is empty!", dir)
            return
        }
        val publishingType = pluginContext.publishingType
        if (!publishingType.isPresent) {
            publishingType.set(PublishingType.USER_MANAGED)
        }
        createBundleForAllGroups(
            publishingType.get().name,
            File(dir.absolutePath, bundleFileName),
            groups,
        )
    }

    private fun createBundleForAllGroups(
        publishingType: String,
        bundle: File,
        groups: Array<File>,
    ) {
        val useCookies =
            !pluginContext.username.isPresent && !pluginContext.password.isPresent && !pluginContext.tokenXml.isPresent
        for (group in groups) {
            val deploymentName = createBundleZip(bundle, group)
            publishComponent(deploymentName.get(), bundle, publishingType, useCookies)
        }
    }

    private fun createBundleZip(
        bundle: File,
        dir: File,
    ): AtomicReference<String> {
        val deploymentName = AtomicReference(bundle.name)
        FileOutputStream(bundle).use {
            BufferedOutputStream(it).use { buffer ->
                val zos = ZipArchiveOutputStream(buffer)
                zos.setLevel(Deflater.BEST_COMPRESSION)
                addDirToZip(deploymentName, dir, zos)
                zos.close()
            }
        }
        return deploymentName
    }

    private fun addDirToZip(
        deploymentName: AtomicReference<String>, dir: File, zos: ZipArchiveOutputStream
    ) {
        val pomExt = ".pom"
        val parent = dir.toPath().parent.toAbsolutePath()
        val builder = ProcessBuilder()
        FileUtils.listFiles(dir, null, true).forEach {
            val addName = it.name.endsWith(pomExt, ignoreCase = true)
            if (addName) {
                deploymentName.set(FilenameUtils.getBaseName(it.name))
            }
            if (it.isFile) {
                val ascFile = autoSign(builder, it.absolutePath)
                val entry = ZipArchiveEntry(parent.relativize(it.toPath()).toString())
                entry.unixMode = 32768 or 420
                zos.putArchiveEntry(entry)
                IOUtils.copy(it.toURI().toURL(), zos)
                zos.closeArchiveEntry()
                if (ascFile != null) {
                    val ascEntry = ZipArchiveEntry(parent.relativize(ascFile.toPath()).toString())
                    entry.unixMode = 32768 or 420
                    zos.putArchiveEntry(ascEntry)
                    IOUtils.copy(ascFile.toURI().toURL(), zos)
                    zos.closeArchiveEntry()
                }
            }
        }
    }

    private fun autoSign(builder: ProcessBuilder, filePath: String): File? {
        if (!FilenameUtils.getExtension(filePath).equals("asc", ignoreCase = true)) {
            try {
                if (builder.command("gpg", "--armor", "--detach-sign", filePath).start()
                        .waitFor() == 0
                ) {
                    val ascFile = File("${filePath}.asc")
                    if (ascFile.isFile) {
                        return ascFile
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return null
    }

    private fun getDeploymentIdFromJson(jsonText: String): String? {
        val index = jsonText.indexOf("deploymentId")
        if (index != -1) {
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?> = Gson().fromJson(jsonText, mapType)
            if (map.containsKey("deploymentId")) {
                val objId = map["deploymentId"]
                return objId as String?
            }
        }
        return null
    }

    private fun saveLastDeploymentId(rawText: String) {
        var respText = rawText
        try {
            val id = getDeploymentIdFromJson(rawText)
            if (id is String) {
                respText = id
            }
        } catch (_: Throwable) {
        }
        val lastDeployment = lastDeploymentsId.toFile()
        val projectDir = lastDeployment.parentFile
        if (!projectDir.exists()) return
        FileUtils.write(lastDeployment, respText, Charset.defaultCharset())
        val ignore = File(projectDir, ".gitignore")
        if (ignore.exists()) {
            val allLines = FileUtils.readLines(
                ignore,
                Charset.defaultCharset(),
            )
            if (allLines.find { line -> line.contains(lastDeployment.name, ignoreCase = true) }
                    .isNullOrEmpty()) {
                allLines.add(lastDeployment.name)
            }
            FileUtils.writeLines(ignore, allLines)
        } else {
            FileUtils.write(
                ignore,
                lastDeployment.name,
                Charset.defaultCharset(),
            )
        }
    }

    private fun buildRequest(
        deploymentName: String, bundle: File, publishingType: String, useCookies: Boolean
    ): Request {
        if (useCookies) {
            return request.url(
                buildUrl().addPathSegments("api/internal/publisher/uploadFile").build()
            ).post(
                MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("deploymentName", deploymentName)
                    .addFormDataPart("description", "").addFormDataPart(
                        "file", bundle.name, bundle.asRequestBody()
                    ).build()
            ).build()
        } else {
            return request.url(
                buildUrl().addPathSegments("api/v1/publisher/upload")
                    .addEncodedQueryParameter("name", deploymentName)
                    .addQueryParameter("publishingType", publishingType).build(),
            ).post(
                MultipartBody.Builder().addFormDataPart(
                    FilenameUtils.getBaseName(bundleFileName),
                    bundleFileName,
                    bundle.asRequestBody(),
                ).build(),
            ).build()
        }
    }

    private fun publishComponent(
        deploymentName: String, bundle: File, publishingType: String, useCookies: Boolean
    ) {
        logger.lifecycle("Processing, please wait...{}", System.lineSeparator())
        client.newCall(
            buildRequest(deploymentName, bundle, publishingType, useCookies)
        ).execute().use {
            val respText = it.body.string()
            if (!it.isSuccessful) {
                logger.error("{}: {}", it.code, respText)
            } else {
                var cookiesMsg = ""
                if (useCookies) {
                    cookiesMsg = " (Authenticate via Cookies)"
                }
                if (publishingType == PublishingType.USER_MANAGED.name) {
                    logger.lifecycle("Upload successful!$cookiesMsg" + System.lineSeparator())
                    publishMsg()
                }
                if (respText.isNotEmpty()) {
                    saveLastDeploymentId(respText)
                }
            }
        }
    }
}
