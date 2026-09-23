package cn.lalaki.pub

import cn.lalaki.pub.PublishingType as PublishingTypeFeature

open class BaseCentralPortalPlusExtension {

    /**
     * The class move to cn.lalaki.pub.PublishingType
     */
    @Deprecated(
        message = "PublishingType has been moved to cn.lalaki.pub.PublishingType",
        replaceWith = ReplaceWith(
            expression = "PublishingType",
            imports = ["cn.lalaki.pub.PublishingType"]
        ),
        level = DeprecationLevel.WARNING
    )
    typealias PublishingType = PublishingTypeFeature
}
