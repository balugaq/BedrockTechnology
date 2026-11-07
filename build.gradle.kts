import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

group = "io.github.ctimet"
version = project.property("version").toString()

// 本机 JDK 与 CI JDK 可能不一致：不使用 toolchain，仅用 release 锁定字节码版本
// （Slimefun4 2025.1 的 maven.compiler.target 为 17）
java {
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    // 用 spigot-api 而非 paper-api：paper-api 带有 Gradle 模块元数据（声明需 Java 21），
    // 与本项目 Java 17 目标字节码的变体匹配冲突；spigot-api 只有 POM 元数据，无此问题
    compileOnly(libs.spigot.api)
    compileOnly(libs.slimefun4)

    // 这些库会被 shade 进插件 jar（对应原 pom 的 compile scope）
    implementation(libs.guizhanlib)
    implementation(libs.mysql)
    implementation(libs.druid)

    compileOnly(libs.jsr305)
    compileOnly(libs.commons.lang3)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version.toString())
        }
    }

    shadowJar {
        archiveBaseName.set("BedrockTechnology")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")

        minimize()

        relocate("net.guizhanss.guizhanlib", "io.github.ctimet.bedrocktechnology.guizhanlib")
        relocate("com.alibaba.druid", "io.github.ctimet.bedrocktechnology.druid")

        exclude("META-INF/*")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        dependsOn(shadowJar)
        val run = file(providers.gradleProperty("server.run.dir").orElse("run"))
        runDirectory.set(run)

        doFirst {
            run.resolve("eula.txt").writeText("eula=true")

            val pl = run.resolve("plugins")
            pl.mkdirs()
            copy {
                from(projectDir.resolve("build/libs")) {
                    include("BedrockTechnology-${project.version}.jar")
                }
                into(pl)
            }
        }

        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dnet.kyori.adventure.text.warn_when_legacy_formatting_detected=false"
        )
        maxHeapSize = "4G"
        minecraftVersion("1.21.1")
    }
}

defaultTasks("clean", "build")
