plugins {
    kotlin("jvm") version "2.1.0"
}

group = "person.shiLiCheng"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // 核心功能
    implementation("org.apache.poi:poi:5.3.0")
    // 处理xlsx文件（Office Open XML格式）
    implementation("org.apache.poi:poi-ooxml:5.4.0")
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