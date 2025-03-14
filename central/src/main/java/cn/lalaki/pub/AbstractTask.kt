package cn.lalaki.pub

import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType
import okhttp3.Credentials.basic
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.toPath

abstract class AbstractTask : DefaultTask() {
    private val sProject by lazy {
        CentralPortalPlusPlugin.sProject!!
    }

    @get:Internal
    val plugin: CentralPortalPlusPlugin by lazy {
        sProject.plugins.getPlugin(CentralPortalPlusPlugin::class.java)
    }

    @get:Internal
    val client by lazy { OkHttpClient() }

    private fun findValueByTagName(doc: org.w3c.dom.Document, nodeName: String): String? {
        val nodes = doc.documentElement.getElementsByTagName(nodeName)
        if (nodes.length > 0) {
            val item = nodes.item(0)
            if (item != null) {
                return item.textContent
            }
        }
        return null
    }

    @get:Internal
    val request by lazy {
        var username = plugin.username
        var password = plugin.password
        val tokenXml = plugin.tokenXml?.toPath()
        if (tokenXml?.isRegularFile() == true) {
            try {
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(tokenXml.toFile())
                username = findValueByTagName(doc, "username")
                password = findValueByTagName(doc, "password")
            } catch (e: IOException) {
                logger.error(e.localizedMessage)
            } catch (e: SAXException) {
                logger.error(e.localizedMessage)
            }
        }
        if (username == null || password == null) {
            throw SecurityException("No username or password set.")
        } else {
            Request.Builder().addHeader(
                "Authorization",
                basic(
                    username!!,
                    password!!,
                ).replace("Basic", "Bearer"),
            )
        }
    }

    @get:Internal
    val lastDeploymentsId by lazy {
        Path(sProject.layout.projectDirectory.asFile.canonicalPath, ".lastDeploymentsId")
    }

    @get:Internal
    val idArg by lazy {
        val arg = "utId"
        if (sProject.hasProperty(arg)) {
            sProject.property(arg).toString().trim()
        } else {
            ""
        }
    }

    fun publishMsg() {
        logger.lifecycle(
            "Due to the artifact's publishingType being {}" +
                "Final confirmation is required on the sonatype's central portal:{}" +
                "https://central.sonatype.com/publishing/deployments",
            PublishingType.USER_MANAGED.name + System.lineSeparator() + System.lineSeparator(),
            System.lineSeparator(),
        )
    }

    fun buildUrl() =
        HttpUrl
            .Builder()
            .scheme("https")
            .host("central.sonatype.com")
}
