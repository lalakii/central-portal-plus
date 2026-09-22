package cn.lalaki.pub

import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Deflater
import javax.inject.Inject
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath

@Suppress("NewApi")
abstract class MiniPublisherTask : AbstractTask {
    @Suppress("unused")
    @Inject
    constructor(e: MiniCentralExtension) : super(e)

    override fun launch() {
        val publishingType = ext.publishingType.get()
        val snapshot = publishingType == PublishingType.SNAPSHOT
        val version = ext.version.get()
        if (version != "unspecified") {
            if (snapshot && !version.endsWith(
                    "-snapshot",
                    ignoreCase = true
                )
            ) {
                throw IllegalArgumentException(langStr("snapshot_error", version))
            } else if (!snapshot && version.endsWith(
                    "-snapshot",
                    ignoreCase = true
                )
            ) {
                throw IllegalArgumentException(langStr("release_error", version))
            }
        }
        if (ext.repository.isPresent) {
            loadTokenXml()
            ext.repository.get().forEach {
                if (snapshot) {
                    publishFiles(it.toPath().toFile())
                } else {
                    publishDir(it.toPath())
                }
            }
        }
    }

    private fun publishDir(dir: Path) {
        if (dir.isDirectory()) {
            val postBundleName = AtomicReference(ext.bundleName.get())
            val bundle = createZipBundle(dir.toFile(), postBundleName)
            if (bundle.isFile) {
                publishing(createRequest(bundle, postBundleName.get(), null), bundle)
            }
        }
    }

    private fun createZipBundle(dir: File, postBundleName: AtomicReference<String>): File {
        val bundle = dir.resolve(postBundleName.get())
        BufferedOutputStream(bundle.outputStream()).use {
            val zos = ZipArchiveOutputStream(BufferedOutputStream(it))
            zos.setLevel(Deflater.BEST_COMPRESSION)
            val sos = ScatterZipOutputStream.fileBased(
                File.createTempFile(".bundle-${System.currentTimeMillis()}", ".tmp")
            )
            signArtifacts(dir, bundle.name)
            addFilesToZip(dir, sos, postBundleName, bundle.name)
            sos.writeTo(zos)
            zos.close()
        }
        return bundle
    }

    private fun signArtifacts(
        dir: File, bundleName: String
    ) {
        val executor = threadPool("miniCentral-Gpg-signing")
        val builder = ProcessBuilder()
        FileUtils.listFiles(
            dir, FileFilterUtils.notFileFilter(
                SuffixFileFilter(
                    "asc", "md5", "sha1", "sha256", "sha512"
                ).or(NameFileFilter("maven-metadata.xml", bundleName)),
            ), TrueFileFilter.INSTANCE
        ).forEach {
            signUseGpgCmd(builder, it)
        }
        executor.shutdown()
        executor.awaitTermination(MAX_POOL_SHUTDOWN_WAIT_SEC, TimeUnit.SECONDS)
    }

    private fun signUseGpgCmd(command: ProcessBuilder, file: File) {
        if (!File(file.absolutePath + ".asc").exists()) {
            var errorCount = 0
            while (errorCount < MAX_RETRY_COUNT) {
                try {
                    val signProc = command.command(
                        "gpg", "--yes", "--armor", "--detach-sign", file.absolutePath
                    ).start()
                    if (signProc.waitFor(
                            PROC_WAIT_TIMEOUT_SEC,
                            TimeUnit.SECONDS
                        ) && signProc.exitValue() == 0
                    ) {
                        break
                    }
                } catch (_: Throwable) {
                }
                errorCount++
            }
        }
    }

    private fun addFilesToZip(
        dir: File,
        sos: ScatterZipOutputStream,
        postBundleName: AtomicReference<String>,
        bundleName: String
    ) {
        var readPom = true
        FileUtils.listFiles(
            dir, FileFilterUtils.notFileFilter(
                SuffixFileFilter(
                    "sha256", "sha512"
                ).or(
                    NameFileFilter(
                        "maven-metadata.xml",
                        "maven-metadata.xml.md5",
                        "maven-metadata.xml.sha1",
                        bundleName
                    )
                ),
            ), TrueFileFilter.INSTANCE
        ).forEach {
            if (it.isFile) {
                if (readPom && FilenameUtils.getExtension(it.name) == "pom") {
                    val artifactName = findDomTagValue(it.toPath(), "name")
                    if (artifactName != null) {
                        postBundleName.set(
                            "$artifactName - " + findDomTagValue(
                                it.toPath(),
                                "version"
                            )
                        )
                        readPom = false
                    }
                }
                val entry = ZipArchiveEntry(it.toRelativeString(dir))
                entry.method = Deflater.DEFLATED
                entry.unixMode = DEFAULT_FILE_PERMS
                it.inputStream().use { ins ->
                    sos.addArchiveEntry(ZipArchiveEntryRequest.createZipArchiveEntryRequest(entry) { ins })
                }
            }
        }
    }

    private fun createRequest(
        bundle: File, postBundleName: String, subUrlPath: String?
    ): Request {
        var request: Request?
        val publishingType = ext.publishingType.get().name
        val snapshot = publishingType == PublishingType.SNAPSHOT.name
        if (ext.username.isPresent && ext.password.isPresent) {
            val requestBuilder =
                tokenRequestBuilder(snapshot, ext.username.get(), ext.password.get())
                    .url(
                        publishUrlCreate(
                            snapshot, postBundleName, publishingType, subUrlPath
                        )
                    )
            request = if (snapshot) {
                requestBuilder.put(bundle.asRequestBody()).build()
            } else {
                requestBuilder.post(
                    MultipartBody.Builder().addFormDataPart(
                        FilenameUtils.getBaseName(bundle.name),
                        bundle.name,
                        bundle.asRequestBody(),
                    ).build()
                ).build()
            }
        } else if (ext.cookies.isPresent) {
            if (snapshot) {
                throw IllegalArgumentException(langStr("cookies_unsupported"))
            } else {
                request = cookiesRequestBuilder(ext.cookies.getOrElse(""))
                    .url(publishUrlCreate(false, null, publishingType))
                    .post(
                        MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("deploymentName", postBundleName)
                            .addFormDataPart("description", "")
                            .addFormDataPart(
                                "file", bundle.name, bundle.asRequestBody()
                            )
                            .build()
                    )
                    .build()
            }
        } else {
            throw IllegalArgumentException(langStr("unset_token"))
        }
        return request
    }

    private fun publishing(request: Request, bundle: File) {
        val publishingType = ext.publishingType.get()
        HTTP_CLIENT.newCall(request).execute().use {
            val content = it.body.string()
            if (it.isSuccessful) {
                if (ext.autoClean.get()) {
                    try {
                        bundle.delete()
                    } catch (_: Throwable) {
                    }
                }
                handleSuccess(publishingType, it.code, request)
            } else {
                if (publishingType == PublishingType.SNAPSHOT) {
                    logger.error("[{}-SNAPSHOT] {}", it.code, request.url.toString())
                } else {
                    it.headers.forEach { value ->
                        logger.error(value.first + " => " + value.second)
                    }
                    logger.error("{}{}: {}", System.lineSeparator(), it.code, content)
                }
            }
        }
    }

    private fun handleSuccess(
        publishingType: PublishingType,
        code: Int,
        request: Request
    ) {
        when (publishingType) {
            PublishingType.USER_MANAGED -> {
                if (!silent()) {
                    logger.lifecycle(
                        langStr("publish_success"),
                        System.lineSeparator(),
                        publishingType,
                        System.lineSeparator(),
                        System.lineSeparator(),
                        System.lineSeparator(),
                        System.lineSeparator(),
                        DEPLOYMENT_URL
                    )
                }
            }

            PublishingType.SNAPSHOT -> {
                if (!silent()) {
                    logger.lifecycle("[{}-SNAPSHOT] {}", code, request.url.toString())
                }
            }

            else -> {}
        }
    }

    private fun publishFiles(dir: File) {
        if (dir.isDirectory) {
            val executor = threadPool("miniCentral-Snapshot-Put")
            FileUtils.listFiles(
                dir, FileFilterUtils.notFileFilter(
                    SuffixFileFilter(
                        "asc", "md5", "sha1", "sha256", "sha512"
                    ),
                ), TrueFileFilter.INSTANCE
            ).forEach {
                executor.execute {
                    publishing(createRequest(it, it.name, it.toRelativeString(dir)), it)
                }
            }
            executor.shutdown()
            executor.awaitTermination(MAX_POOL_SHUTDOWN_WAIT_SEC, TimeUnit.SECONDS)
        }
    }
}
