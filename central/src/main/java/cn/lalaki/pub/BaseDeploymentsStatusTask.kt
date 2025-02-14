package cn.lalaki.pub

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Response
import okhttp3.internal.EMPTY_REQUEST
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.charset.Charset

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
            getDeploymentStatus(id)
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
                    ).post(EMPTY_REQUEST)
                    .build(),
            ).execute()
            .use {
                handleResponse(it)
            }
    }

    private fun handleResponse(resp: Response) {
        val respText = resp.body?.string().toString()
        if (resp.isSuccessful) {
            try {
                val gson = Gson().newBuilder().setPrettyPrinting().create()
                val obj = gson.fromJson(respText, DeploymentObject::class.java)
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
}
