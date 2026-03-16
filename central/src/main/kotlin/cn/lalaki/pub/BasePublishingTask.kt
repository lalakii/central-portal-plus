package cn.lalaki.pub

import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths

/**
 * Created on 2024-06-20
 *
 * @author lalaki (i@lalaki.cn)
 * @since Classes for publishing artifacts to the publisher API.
 */
@Suppress("NewApi")
abstract class BasePublishingTask : AbstractTask() {
    private val params by lazy {
        ZipParameters().apply {
            compressionLevel = CompressionLevel.ULTRA
            setExcludeFileFilter { it.name.contains("maven-metadata.xml", ignoreCase = true) }
        }
    }
    private val bundleFileName = "bundle.zip"

    /***
     * Creating bundles and publishing
     */
    @TaskAction
    fun launch() {
        val url =
            pluginContext.url ?: throw NullPointerException("missing local maven repo, url=" + null)
        val dir = Paths.get(url).toFile()
        if (!dir.isDirectory) {
            logger.error("local maven repo ({}) is not a folder!", dir)
            return
        }
        val groups = dir.listFiles()
        if (groups == null) {
            logger.error("local maven repo ({}) is empty!", dir)
            return
        }
        var publishingType = pluginContext.publishingType
        if (publishingType == null) {
            publishingType = PublishingType.USER_MANAGED
        }
        createBundleForAllGroups(
            publishingType.name,
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
            pluginContext.username == null && pluginContext.password == null && pluginContext.tokenXml == null
        for (group in groups) {
            val deploymentName = createBundleZip(bundle, group)
            publishComponent(deploymentName, bundle, publishingType, useCookies)
        }
    }

    private fun createBundleZip(
        bundle: File,
        dir: File,
    ): String {
        var deploymentName = bundle.name
        ZipFile(bundle).use { zip ->
            zip.addFolder(dir, params)
            val pomExt = ".pom"
            val pomHeader = zip.fileHeaders.find { it.fileName.endsWith(pomExt, ignoreCase = true) }
            if (pomHeader != null) {
                deploymentName = FilenameUtils.getBaseName(pomHeader.fileName)
            }
        }
        return deploymentName
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
                    .isNullOrEmpty()
            ) {
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
        deploymentName: String,
        bundle: File,
        publishingType: String,
        useCookies: Boolean
    ): Request {
        if (useCookies) {
            return request.url(
                buildUrl().addPathSegments("api/internal/publisher/uploadFile").build()
            )
                .post(
                    MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("deploymentName", deploymentName)
                        .addFormDataPart("description", "")
                        .addFormDataPart(
                            "file",
                            bundle.name,
                            bundle.asRequestBody()
                        )
                        .build()
                )
                .build()
        } else {
            return request.url(
                buildUrl().addPathSegments("api/v1/publisher/upload")
                    .addEncodedQueryParameter("name", deploymentName)
                    .addQueryParameter("publishingType", publishingType)
                    .build(),
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
        deploymentName: String,
        bundle: File,
        publishingType: String,
        useCookies: Boolean
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
