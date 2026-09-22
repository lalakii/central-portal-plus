package cn.lalaki.pub

import javax.inject.Inject

abstract class MiniDeleteTask : AbstractTask {
    @Suppress("unused")
    @Inject
    constructor(e: MiniCentralExtension) : super(e)

    @Suppress("NewApi")
    override fun launch() {
        findDeploymentId()?.apply {
            loadTokenXml()
            HTTP_CLIENT.newCall(
                tokenRequestBuilder(false, ext.username.get(), ext.password.get()).url(
                    deleteUrlCreate(this)
                ).delete().build()
            ).execute().use {
                if (it.isSuccessful) {
                    logger.lifecycle("[{}] {}", it.code, langStr("delete"))
                } else {
                    logger.lifecycle(
                        langStr("delete_failed"),
                        it.code,
                        System.lineSeparator(),
                        System.lineSeparator(),
                        System.lineSeparator(),
                        System.lineSeparator()
                    )
                }
            }
        }
    }
}
