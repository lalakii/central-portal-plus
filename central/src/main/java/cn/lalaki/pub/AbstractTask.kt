package cn.lalaki.pub

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import okhttp3.Credentials.basic
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import java.nio.charset.Charset
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.toPath

abstract class AbstractTask : DefaultTask() {
    @get:Internal
    val plugin: CentralPortalPlusPlugin by lazy {
        project.plugins.getPlugin(CentralPortalPlusPlugin::class.java)
    }

    @get:Internal
    val client by lazy { OkHttpClient() }

    @get:Internal
    val request by lazy {
        var username = plugin.username
        var password = plugin.password
        val tokenXml = plugin.tokenXml?.toPath()
        if (tokenXml?.isRegularFile() == true) {
            try {
                val xmlText =
                    FileUtils.readFileToString(tokenXml.toFile(), Charset.defaultCharset())
                val userToken =
                    XmlMapper.builder()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()
                        .readValue(xmlText, UserToken::class.java)
                username = userToken.username
                password = userToken.password
            } catch (e: JsonMappingException) {
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
        Path(project.layout.projectDirectory.asFile.canonicalPath, ".lastDeploymentsId")
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
        logger.lifecycle(
            "Due to the artifact's publishingType being {}" +
                    "Final confirmation is required on the sonatype's central portal:{}" +
                    "https://central.sonatype.com/publishing/deployments",
            BaseCentralPortalPlusExtension.PublishingType.USER_MANAGED.name +
                    System.lineSeparator() +
                    System.lineSeparator(),
            System.lineSeparator(),
        )
    }

    fun buildUrl() =
        HttpUrl
            .Builder()
            .scheme("https")
            .host("central.sonatype.com")
}
