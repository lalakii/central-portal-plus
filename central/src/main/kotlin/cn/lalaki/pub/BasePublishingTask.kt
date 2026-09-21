package cn.lalaki.pub

import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.TaskAction
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest
import org.w3c.dom.NodeList
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Deflater
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.toPath

/**
 * Created on 2026-09-21
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
            val zos = ZipArchiveOutputStream(BufferedOutputStream(it))
            zos.setLevel(Deflater.BEST_COMPRESSION)
            val sos = ScatterZipOutputStream.fileBased(
                File.createTempFile(
                    "bundle_${System.nanoTime()}", ".tmp"
                )
            )
            addDirToZip(deploymentName, dir, sos)
            sos.writeTo(zos)
            zos.close()
        }
        return deploymentName
    }

    private fun addDirToZip(
        deploymentName: AtomicReference<String>, dir: File, sos: ScatterZipOutputStream
    ) {
        val pomExt = ".pom"
        val parent = dir.toPath().parent.toAbsolutePath()
        val builder = ProcessBuilder()
        val cores = Runtime.getRuntime().availableProcessors()
        val poolSize = 4.coerceAtLeast(cores) * 2
        val executor = ThreadPoolExecutor(
            poolSize,
            poolSize * 2,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(1024),
            { runnable ->
                Thread(runnable, "central-gpg-signing")
            },
            ThreadPoolExecutor.CallerRunsPolicy()
        )
        val noNeedSign = hashSetOf("asc", "md5", "sha1", "sha256", "sha512")
        // sign
        FileUtils.listFiles(dir, null, true).filter {
            !it.name.startsWith("maven-metadata.xml", ignoreCase = true)
        }.filter {
            it.isFile
        }.filter {
            !noNeedSign.contains(FilenameUtils.getExtension(it.name))
        }.forEach {
            val ascFile = File("${it.absolutePath}.asc")
            if (!ascFile.isFile) {
                executor.execute {
                    signUseGpgCmd(builder, it.absolutePath)
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(300, TimeUnit.SECONDS)
        val noNeedHash = hashSetOf("sha256", "sha512")
        FileUtils.listFiles(dir, null, true).filter {
            !it.name.startsWith("maven-metadata.xml", ignoreCase = true)
        }.filter { !noNeedHash.contains(FilenameUtils.getExtension(it.name)) }.forEach {
            val addName = it.name.endsWith(pomExt, ignoreCase = true)
            if (addName) {
                deploymentName.set(FilenameUtils.getBaseName(it.name))
                try {
                    val document =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
                    document.documentElement.normalize()
                    val nameValue = getNodeText(document.getElementsByTagName("name"));
                    if (nameValue != null) {
                        val versionValue = getNodeText(document.getElementsByTagName("version"));
                        deploymentName.set("$nameValue - $versionValue")
                    }
                } catch (_: Throwable) {
                }
            }
            if (it.isFile) {
                val entry = ZipArchiveEntry(parent.relativize(it.toPath()).toString())
                entry.method = Deflater.DEFLATED
                entry.unixMode = 32768 or 420
                it.inputStream().use { ins ->
                    sos.addArchiveEntry(ZipArchiveEntryRequest.createZipArchiveEntryRequest(entry) { ins })
                }
            }
        }
    }

    private fun getNodeText(nodes: NodeList): String? {
        if (nodes.length > 0) {
            val node = nodes.item(0)
            if (node != null) {
                val text = node.textContent
                if (!text.isNullOrBlank()) {
                    return text
                }
            }
        }
        return null
    }

    private fun signUseGpgCmd(builder: ProcessBuilder, filePath: String) {
        var errorCount = 0
        while (errorCount < 5) {
            try {
                val signProc =
                    builder.command("gpg", "--yes", "--armor", "--detach-sign", filePath).start()
                if (signProc.waitFor(10, TimeUnit.SECONDS) && signProc.exitValue() == 0) {
                    break
                }
            } catch (_: Throwable) {
            }
            errorCount++
        }
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
