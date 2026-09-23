package cn.lalaki.pub

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.net.URI
import javax.inject.Inject

/**
 * Created on 2026-09-22
 *
 * @author lalakii (i@lalaki.cn)
 * @since extension class
 */
@Suppress("unused")
abstract class MiniCentralExtension @Inject constructor(o: ObjectFactory) {
    @get:Input
    @get:Optional
    abstract val username: Property<String>

    @get:Input
    @get:Optional
    abstract val password: Property<String>

    @get:Input
    @get:Optional
    abstract val tokenXml: Property<URI>

    @get:Input
    @get:Optional
    abstract val cookies: Property<String>

    @get:Input
    @get:Optional
    abstract val publishingType: Property<PublishingType>

    @get:Input
    @get:Optional
    abstract val url: Property<URI>

    @get:Input
    @get:Optional
    abstract val useGpgCmd: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val autoClean: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val disabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val quiet: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val connectTimeoutSeconds: Property<Long>

    @get:Input
    @get:Optional
    abstract val readTimeoutSeconds: Property<Long>

    @get:Input
    @get:Optional
    abstract val writeTimeoutSeconds: Property<Long>

    @get:Input
    @get:Optional
    internal abstract val repository: SetProperty<URI>

    @get:Input
    @get:Optional
    internal abstract val bundleName: Property<String>

    @get:Input
    @get:Optional
    internal abstract val version: Property<String>

    @get:Input
    @get:Optional
    internal abstract val args: Property<String>

    @get:Input
    @get:Optional
    internal abstract val languages: MapProperty<Any, Any>

    init {
        args.set("")
        quiet.set(false)
        useGpgCmd.set(true)
        disabled.set(false)
        version.set("1.0.0")
        bundleName.set("bundle.zip")
        autoClean.convention(true)
        publishingType.set(PublishingType.USER_MANAGED)
    }
}
