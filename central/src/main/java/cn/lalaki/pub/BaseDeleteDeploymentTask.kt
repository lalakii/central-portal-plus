package cn.lalaki.pub

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.charset.Charset
@Suppress("NewApi")
abstract class BaseDeleteDeploymentTask : AbstractTask() {
    @TaskAction
    fun launch() {
        var id = idArg
        var lastDeployment: File? = null
        if (id.isEmpty()) {
            lastDeployment = lastDeploymentsId.toFile()
            if (lastDeployment.exists()) {
                id = FileUtils.readFileToString(lastDeployment, Charset.defaultCharset())
            }
        }
        if (id.isEmpty()) return
        client
            .newCall(
                request
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .url(
                        buildUrl()
                            .addPathSegments("api/v1/publisher/deployment")
                            .addPathSegment(id)
                            .build(),
                    ).delete(null)
                    .build(),
            ).execute()
            .use {
                if (it.isSuccessful) {
                    logger.lifecycle("Artifact with deployment id {} has been deleted", id)
                    lastDeployment?.delete()
                } else {
                    logger.error("{}: {}", it.code, it.body.string())
                }
            }
    }
}
