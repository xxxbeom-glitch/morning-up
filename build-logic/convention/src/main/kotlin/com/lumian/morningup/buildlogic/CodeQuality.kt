package com.lumian.morningup.buildlogic

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** ktlint(모양 검사)와 detekt(코드 냄새 검사)를 모든 모듈에 같은 규칙으로 걸어요. */
internal fun Project.configureCodeQuality() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        android.set(true)
        ignoreFailures.set(false)
        filter {
            exclude { it.file.path.contains("${java.io.File.separator}build${java.io.File.separator}") }
        }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom("src/main/kotlin", "src/main/java", "src/test/kotlin", "src/test/java")
    }

    // detekt 1.23은 Kotlin 2.0.21로 만들어졌어요. 프로젝트의 Kotlin 버전이 끼어들지 않게 고정해요.
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(libs.version("detektKotlin"))
            }
        }
    }

    tasks.named("check").configure { dependsOn("detekt") }
}
