import java.util.Properties

plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.36.0"
}
/* ======================= 填写个人信息 ============================= */
/** 从 settings.gradle.kts 文件取值过来 */
val artifactId: String = rootProject.name
/* 组织机构的名称必须是 io.github.<你的github名称>，除非你有你自己的域名; maven中心会校验你是否拥有这个域名，否则一律挂到 github 下 */
group = "io.github.shilic"
/* 版本号  !!! 严禁 -SNAPSHOT */
version = "1.0.3"
/** 提取个人的链接，方便统一修改 */
val myGit: String = "github.com/shilic/smart-grid"
/** 复用我的POM */
val myPom: MavenPom.() -> Unit = {
    name = artifactId
    description = "更聪明的网络字节转换器"
    url = "https://$myGit"
    licenses {
        license {
            name = "The Apache License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        }
    }
    developers {
        developer {
            id = "诚"
            name = "诚"
            email = "985478238@qq.com"
        }
    }
    scm {
        url = "https://$myGit"
        connection = "scm:git:git://$myGit.git"
        developerConnection = "scm:git:ssh://$myGit.git"
    }
}
// 定义仓库，构建脚本会从这里拉取依赖
repositories {
    mavenCentral()
}
/* 使用 mavenPublishing 发布到 Maven Central，签名、源码包、文档包均由插件自动处理 */
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), artifactId, version.toString())
    pom(myPom)
}
/* 追加 GitHubPackages 发布目标; com.vanniktech.maven.publish 插件 已经打包了发布内容，所以这里只需要追加远程仓库。 */
afterEvaluate {
    /* 从 GRADLE_USER_HOME 读取全局 gradle.properties (存放 git 凭证) !!! 不要把密钥放到仓库里上传到 github */
    val globalProps: Properties = Properties().apply {
        gradle.gradleUserHomeDir.resolve("gradle.properties")
            .takeIf(File::exists)?.reader()?.use(::load)
    }
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/shilic/${artifactId}")
                credentials {
                    username = globalProps.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR") ?: ""
                    password = globalProps.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN") ?: ""
                }
            }
        }
    }
}
// 项目依赖
dependencies {
    //testImplementation(kotlin("test"))
    // 核心功能: 处理xlsx文件
    implementation("org.apache.poi:poi:5.5.1")
    // 处理xlsx文件（Office Open XML格式）
    implementation("org.apache.poi:poi-ooxml:5.5.1")
    // 添加 Gson 依赖
    implementation("com.google.code.gson:gson:2.10.1")
    // 使用与您 Kotlin 版本匹配的版本
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation(kotlin("test"))
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}