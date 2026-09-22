package cn.lalaki.pub

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

abstract class MiniDumpTask : AbstractTask {
    @Suppress("unused")
    @Inject
    constructor(e: MiniCentralExtension) : super(e)

    override fun launch() {
        findDeploymentId()?.apply {
            loadTokenXml()
            HTTP_CLIENT.newCall(
                tokenRequestBuilder(false, ext.username.get(), ext.password.get()).url(
                    dumpUrlCreate(this)
                ).post("".toRequestBody()).build()
            ).execute().use {
                logger.lifecycle("[{}]", it.code)
                val type = object : TypeToken<Map<String, Any>>() {}
                val ret = Gson().fromJson(it.body.string(), type)
                if (ret is Map<*, *>) {
                    for (it in ret) {
                        logger.lifecycle("{}: {}", it.key, it.value)
                    }
                }
            }
        }
    }
}
