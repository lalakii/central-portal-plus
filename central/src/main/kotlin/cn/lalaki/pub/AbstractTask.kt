package cn.lalaki.pub

import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.io.input.XmlStreamReader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.isRegularFile
import kotlin.io.path.toPath

abstract class AbstractTask : DefaultTask {
    @get:Input
    abstract var ext: MiniCentralExtension

    constructor(ext: MiniCentralExtension) {
        this.ext = ext
    }

    @TaskAction
    abstract fun launch()
    fun langStr(name: String, vararg args: String): String {
        val map = ext.languages.get()
        if (map.containsKey(name)) {
            val obj = map[name]
            if (obj is String) {
                return String.format(obj, *args)
            }
        }
        return ""
    }

    fun findDeploymentId(): String? {
        val id = ext.args.get()
        if (!id.isBlank()) {
            return id
        }
        return null
    }

    fun loadTokenXml() {
        if (ext.tokenXml.isPresent) {
            val xml = ext.tokenXml.get().toPath()
            findDomTagValue(xml, "username")?.let {
                ext.username.set(it)
            }
            findDomTagValue(xml, "password")?.let {
                ext.password.set(it)
            }
        }
    }

    fun silent() = ext.quiet.isPresent && ext.quiet.getOrElse(false)

    companion object {
        private const val FILE_MODE_0644 = 420
        private const val S_IF_REG = 32_768
        const val DEPLOYMENT_URL = "https://central.sonatype.com/publishing/deployments"
        const val MAX_POOL_SHUTDOWN_WAIT_SEC = 300L
        const val PROC_WAIT_TIMEOUT_SEC = 30L
        const val MAX_RETRY_COUNT = 5
        const val DEFAULT_FILE_PERMS = S_IF_REG or FILE_MODE_0644
        var HTTP_CONNECT_TIMEOUT_SEC = 15L
        var HTTP_READ_TIMEOUT_SEC = 60L
        var HTTP_WRITE_TIMEOUT_SEC = 60L
        val HTTP_CLIENT by lazy {
            OkHttpClient.Builder()
                .connectTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(HTTP_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(HTTP_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
                .build()
        }

        @Suppress("NewApi")
        fun findDomTagValue(pomXml: Path, tagName: String): String? {
            xmlReader(pomXml)?.let {
                try {
                    val nodes = it.getElementsByTagName(tagName)
                    for (i in 0 until nodes.length) {
                        val node = nodes.item(i)
                        val text = node.textContent
                        if (!text.isNullOrBlank()) {
                            return text
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            return null
        }

        fun tokenRequestBuilder(snapshot: Boolean, text0: String, text1: String) =
            Request.Builder().addHeader(
                "Authorization", Credentials.basic(
                    text0, text1,
                ).replace(
                    "Basic", if (snapshot) {
                        "Basic"
                    } else {
                        "Bearer"
                    }
                )
            )

        fun cookiesRequestBuilder(cookies: String) = Request.Builder().addHeader("Cookie", cookies)

        fun deleteUrlCreate(deploymentId: String) =
            urlBuilder().addPathSegments("api/v1/publisher/deployment/")
                .addPathSegments(deploymentId).build()

        fun dumpUrlCreate(deploymentId: String) =
            urlBuilder().addPathSegments("/api/v1/publisher/status")
                .addQueryParameter("id", deploymentId).build()

        fun publishUrlCreate(
            snapshot: Boolean,
            deploymentName: String? = null,
            publishingType: String,
            subUrlPath: String? = null
        ): HttpUrl {
            val builder = urlBuilder()
            if (deploymentName == null) {
                builder.addPathSegments("api/internal/publisher/uploadFile")
            } else {
                if (snapshot) {
                    builder.addPathSegments("repository/maven-snapshots/")
                    if (subUrlPath != null) {
                        builder.addPathSegments(subUrlPath)
                    }
                } else {
                    builder.addPathSegments("api/v1/publisher/upload")
                        .addEncodedQueryParameter("name", deploymentName)
                        .addQueryParameter("publishingType", publishingType)
                }
            }
            return builder.build()
        }

        @Suppress("NewApi")
        fun xmlReader(file: Path): Element? {
            if (file.isRegularFile()) {
                try {
                    return XmlStreamReader.builder().setPath(file).get().use {
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                            InputSource(
                                it
                            )
                        ).documentElement
                    }
                } catch (_: Throwable) {
                }
            }
            return null
        }

        fun threadPool(tag: String): ThreadPoolExecutor {
            val n = Runtime.getRuntime().availableProcessors()
            val poolSize = 4.coerceAtLeast(n) * 2
            val executor = ThreadPoolExecutor(
                poolSize, poolSize * 4, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(1024), {
                    Thread(it, tag)
                }, ThreadPoolExecutor.CallerRunsPolicy()
            )
            return executor
        }

        private fun urlBuilder() = HttpUrl.Builder().scheme("https").host("central.sonatype.com")
    }
}
