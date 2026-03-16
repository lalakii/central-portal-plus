package cn.lalaki.pub

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.charset.Charset

@Suppress("NewApi")
abstract class BaseDeploymentsStatusTask : AbstractTask() {
    @TaskAction
    fun launch() {
        var id = idArg
        if (id.isEmpty()) {
            val lastDeployment = lastDeploymentsId.toFile()
            if (lastDeployment.exists()) {
                id = FileUtils.readFileToString(lastDeployment, Charset.defaultCharset())
            }
        }
        if (id.isNotEmpty()) {
            val useCookies =
                pluginContext.username == null && pluginContext.password == null && pluginContext.tokenXml == null
            if (useCookies) {
                getDeploymentStatusWithCookies(id)
            } else {
                getDeploymentStatus(id)
            }
        }
    }

    private fun getDeploymentStatus(id: String) {
        client
            .newCall(
                request
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .url(
                        buildUrl()
                            .addPathSegments("api/v1/publisher/status")
                            .addEncodedQueryParameter("id", id)
                            .build(),
                    )
                    .post("".toRequestBody())
                    .build(),
            )
            .execute()
            .use {
                handleResponse(it, id)
            }
    }

    private fun getDeploymentStatusWithCookies(id: String) {
        client
            .newCall(
                request
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .url(
                        buildUrl()
                            .addPathSegments("api/internal/publisher/deployments")
                            .build(),
                    )
                    .post("{\"page\":0,\"size\":25,\"sortField\":\"createTimestamp\",\"sortDirection\":\"desc\"}".toRequestBody())
                    .build(),
            )
            .execute()
            .use {
                handleResponse(it, id)
            }
    }

    private fun handleMultiResponse(gson: Gson, respText: String, id: String): DeploymentObject? {
        val myObj = gson.fromJson(respText, DeploymentResponse::class.java)
        val deployments = myObj.deployments
        for (it in deployments) {
            if (it.deploymentId == id) {
                val deployment = DeploymentObject()
                deployment.deploymentId = it.deploymentId
                deployment.deploymentName = it.deploymentName
                deployment.deploymentState = it.deploymentState
                return deployment
            }
        }
        return null
    }

    private fun handleResponse(resp: Response, id: String) {
        val respText = resp.body.string()
        if (resp.isSuccessful) {
            val gson = Gson().newBuilder().setPrettyPrinting().create()
            var obj: DeploymentObject? = null
            try {
                obj = handleMultiResponse(gson, respText, id)
            } catch (_: Throwable) {
            }
            try {
                if (obj == null) {
                    obj = gson.fromJson(respText, DeploymentObject::class.java)
                }
                logger.lifecycle(
                    "Deployment info{}{}Id: {}{}Name: {}{}State: {}{}",
                    System.lineSeparator(),
                    System.lineSeparator(),
                    obj.deploymentId,
                    System.lineSeparator(),
                    obj.deploymentName,
                    System.lineSeparator(),
                    obj.deploymentState,
                    System.lineSeparator(),
                )
                if (obj.deploymentState
                        .toString()
                        .contains("VALIDATED", ignoreCase = true)
                ) {
                    publishMsg()
                }
            } catch (e: IOException) {
                logger.error(e.localizedMessage)
            }
        } else {
            logger.error("{}: Id={} {}", resp.code, idArg, respText)
        }
    }

    data class DeploymentObject(
        @SerializedName("deploymentId") var deploymentId: String? = null,
        @SerializedName("deploymentName") var deploymentName: String? = null,
        @SerializedName("deploymentState") var deploymentState: String? = null,
        @SerializedName("purls") var purls: ArrayList<String> = arrayListOf(),
        @SerializedName("errors") var errors: Any? = null,
    )

    data class DeploymentResponse(
        @SerializedName("deployments")
        val deployments: List<Deployment>
    )

    data class Deployment(
        @SerializedName("deploymentId")
        val deploymentId: String,

        @SerializedName("deploymentName")
        val deploymentName: String,

        @SerializedName("deploymentState")
        val deploymentState: String,

        @SerializedName("createTimestamp")
        val createTimestamp: Long,

        @SerializedName("updateTimestamp")
        val updateTimestamp: Long,

        @SerializedName("deployedComponentVersions")
        val deployedComponentVersions: List<Any>? = null,

        @SerializedName("coordinates")
        val coordinates: List<Coordinate>
    )

    data class Coordinate(
        @SerializedName("groupId")
        val groupId: String,

        @SerializedName("artifactId")
        val artifactId: String,

        @SerializedName("version")
        val version: String
    )
}
