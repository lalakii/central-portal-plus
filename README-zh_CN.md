# CentralPortalPlus

[![Maven Central](https://img.shields.io/maven-central/v/cn.lalaki.central/central.svg?label=Maven%20Central&logo=sonatype)](https://central.sonatype.com/artifact/cn.lalaki.central/central/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6ca71f005bd44dc4893761c16007b0ea)](https://app.codacy.com/gh/lalakii/central-portal-plus/dashboard)
![License: Apache-2.0 (shields.io)](https://img.shields.io/badge/License-Apache--2.0-c02041?logo=apache)

[<img src="https://fastly.jsdelivr.net/gh/lalakii/central-portal-plus@master/icon.png" width="64" alt="CentralPortalPlus">](https://central.sonatype.com/artifact/cn.lalaki.central/central)

语言: [English](./README.md) | [简体中文](./README-zh_CN.md)

此插件实现了 Sonatype 的 [中央仓库发布 API](https://central.sonatype.com/api-doc) (part
of).

简单快速的发布 maven-publish 生成的 Artifact 到 [Sonatype 的中央仓库](https://central.sonatype.com/)

提示: 这是一个第三方插件

**新变化**

- **SNAPSHOT**: 支持发布以"-SNAPSHOT"为后缀的快照版本
- **Cache**: 适配 Gradle 配置缓存

## 如何使用

在 Gradle 项目中添加:

```kts
plugins {
    id("cn.lalaki.central") version "3.0.3"
}
```

插件配置:

```kts
centralPortalPlus {
    /**   -- 身份认证配置 -- 
     * 注意，这里不要直接将账号密码填写到文件
     * 推荐使用 System.getenv("YOUR_ENV") 从系统环境变量加载
    */
    
    // 配置方式1: 配置你的 Token.
    username = "..."
    password = "..."
    
    // 配置方式2: 从 XML 加载
    tokenXml = uri("D:\\user_token.xml")
    
    // 配置方式3: 直接从网站把 Cookies 复制下来，此方式不支持快照发布
    cookies = "..."
    // 登录 https://central.sonatype.com 检查 HTTP 请求头部即可获取 Cookies

    /** 找不到类就 import cn.lalaki.pub.PublishingType
    1. PublishingType.AUTOMATIC  正式版自动发布
    2. PublishingType.USER_MANAGED  正式版手动发布
    3. PublishingType.SNAPSHOT  快照自动发布，无手动选项
     */
    publishingType = PublishingType.USER_MANAGED
}
```

<p align="center">( 关于 "user_token.xml"，保存你的令牌到 XML文档。 )</p>
<p align="center">
<img src="https://fastly.jsdelivr.net/gh/lalakii/lalakii.github.io@master/tokenXml.jpg" width="450" alt="tokenXml">
</p>

像以前一样配置 maven-publish，
文档: [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html), 或者查看：[sample/build.gradle.kts](https://github.com/lalakii/central-portal-plus/blob/master/sample/build.gradle.kts)

```kts
val localMavenRepo =
    uri("path/of/local_repo") // 建议指定一个本地空文件夹

publishing {
    repositories {
        maven {
            url = localMavenRepo // 本地空文件夹路径
        }

        // or

        mavenLocal() // 不想自己指定文件夹，也可以直接 mavenLocal()
    }
    // ...
}
```

现在你可以在命令行尝试发布

```console
REM ---- On Windows ----
.\gradlew publishToCentralPortal
REM ----     或     ----
.\gradlew publish
```
```shell
# Linux / Unix / macOS
./gradlew publishToCentralPortal
#       或
./gradlew publish
```

如果你需要检查你的部署，推荐到 Web 端查看更直观

```console
.\gradlew dumpDeployment -PUTID="deployment Id"
```

如果你需要删除部署

```console
.\gradlew deleteDeployment -PUTID="deployment Id"
```

注意：已经发布成功的部署是无法被删除的。

您可以在官方网站上了解你的部署情况: [Maven Central: Publishing](https://central.sonatype.com/publishing/deployments)

## 许可证
[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)