package cn.lalaki.pub

import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import okhttp3.MultipartBody
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
        val url = pluginContext.url ?: throw NullPointerException("missing local maven repo, url=" + null)
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
        for (group in groups) {
            publishComponent(publishingType, createBundleZip(bundle, group), bundle)
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

    private fun saveLastDeploymentId(respText: String) {
        val lastDeployment = lastDeploymentsId.toFile()
        val projectDir = lastDeployment.parentFile
        if (!projectDir.exists()) return
        FileUtils.write(lastDeployment, respText, Charset.defaultCharset())
        val ignore = File(projectDir, ".gitignore")
        if (ignore.exists()) {
            val allLines =
                FileUtils
                    .readLines(
                        ignore,
                        Charset.defaultCharset(),
                    )
            if (allLines
                    .find { line -> line.contains(lastDeployment.name, ignoreCase = true) }
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

    private fun publishComponent(
        publishingType: String,
        deploymentName: String,
        bundle: File,
    ) {
        client
            .newCall(
                request
                    .url(
                        buildUrl()
                            .addPathSegments("api/v1/publisher/upload")
                            .addEncodedQueryParameter("name", deploymentName)
                            .addQueryParameter("publishingType", publishingType)
                            .build(),
                    ).post(
                        MultipartBody
                            .Builder()
                            .addFormDataPart(
                                FilenameUtils.getBaseName(bundleFileName),
                                bundleFileName,
                                bundle.asRequestBody(),
                            ).build(),
                    ).build(),
            ).execute()
            .use {
                val respText = it.body.string()
                if (!it.isSuccessful) {
                    logger.error("{}: {}", it.code, respText)
                } else {
                    if (publishingType == PublishingType.USER_MANAGED.name) {
                        logger.lifecycle("Upload successful!" + System.lineSeparator())
                        publishMsg()
                    }
                    if (respText.isNotEmpty()) {
                        saveLastDeploymentId(respText)
                    }
                }
            }
    }
}
