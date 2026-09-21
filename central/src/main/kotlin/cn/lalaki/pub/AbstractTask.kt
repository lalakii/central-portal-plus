package cn.lalaki.pub

import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType
import okhttp3.Credentials.basic
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path
import kotlin.io.path.toPath

@Suppress("NewApi")
abstract class AbstractTask : DefaultTask() {
    @get:Internal
    abstract var pluginContext: CentralPortalPlusPlugin

    @get:Internal
    val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(pluginContext.connectTimeoutSeconds.getOrElse(CONNECT_TIMEOUT_DEFAULT), TimeUnit.SECONDS)
            .readTimeout(pluginContext.readTimeoutSeconds.getOrElse(READ_TIMEOUT_DEFAULT), TimeUnit.SECONDS)
            .writeTimeout(pluginContext.writeTimeoutSeconds.getOrElse(WRITE_TIMEOUT_DEFAULT), TimeUnit.SECONDS)
            .build()
    }

    @get:Internal
    val request by lazy {
        var username = pluginContext.username
        var password = pluginContext.password
        val tokenXml = pluginContext.tokenXml
        if (tokenXml.isPresent) {
            try {
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(tokenXml.get().toPath().toFile())
                username.set(findValueByTagName(doc, "username"))
                password.set(findValueByTagName(doc, "password"))
            } catch (e: IOException) {
                logger.error(e.localizedMessage)
            } catch (e: SAXException) {
                logger.error(e.localizedMessage)
            }
        }
        val cookies = pluginContext.cookies
        if (!username.isPresent || !password.isPresent) {
            if (cookies.isPresent) {
                return@lazy Request.Builder().addHeader("Cookie", cookies.getOrElse(""))
            }
            throw SecurityException("No username or password set.")
        }
        Request.Builder().addHeader(
            "Authorization",
            basic(
                username.get(),
                password.get(),
            ).replace("Basic", "Bearer"),
        )
    }

    @get:Internal
    val lastDeploymentsId by lazy {
        Path(pluginContext.workDir, ".lastDeploymentsId")
    }

    @get:Internal
    val idArg by lazy {
        val arg = "utId"
        if (project.hasProperty(arg)) {
            project.property(arg).toString().trim()
        } else {
            ""
        }
    }

    fun publishMsg() {
        val deploymentUrl = "https://central.sonatype.com/publishing/deployments"
        logger.lifecycle(
            "Due to the artifact's " + "publishingType being {}{}{}" +
                    "Final confirmation is required" +
                    " on the sonatype's central portal: " +
                    "{}{}",
            PublishingType.USER_MANAGED.name,
            System.lineSeparator(),
            System.lineSeparator(),
            System.lineSeparator(),
            deploymentUrl
        )
    }

    fun buildUrl() = HttpUrl.Builder().scheme("https").host("central.sonatype.com")

    private fun findValueByTagName(doc: org.w3c.dom.Document, nodeName: String): String? {
        val nodes = doc.documentElement.getElementsByTagName(nodeName)
        var item: Node? = null
        if (nodes.length > 0) {
            item = nodes.item(0)
        }
        return item?.textContent
    }

    companion object {
        const val READ_TIMEOUT_DEFAULT = 120L
        const val WRITE_TIMEOUT_DEFAULT = 120L
        const val CONNECT_TIMEOUT_DEFAULT = 30L
    }
}
