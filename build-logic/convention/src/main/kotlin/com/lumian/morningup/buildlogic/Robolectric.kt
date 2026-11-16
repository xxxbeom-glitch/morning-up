package com.lumian.morningup.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

/**
 * Robolectric이 테스트 중에 안드로이드 jar를 직접 내려받지 않게 해요.
 * Gradle이 받아 둔 jar를 넘기고 오프라인으로 돌려서, 테스트 JVM의 네트워크 사정과 상관없이 같은 결과가 나와요.
 */
internal fun Project.configureRobolectricOffline() {
    val jars = configurations.create("robolectricAndroidAll") { isTransitive = false }
    dependencies.add(jars.name, libs.lib("robolectric-android-all"))
    val dir = layout.buildDirectory.dir("robolectric-jars")
    val copy = tasks.register("copyRobolectricJars", Sync::class.java) {
        from(jars)
        into(dir)
    }
    tasks.withType(Test::class.java).configureEach {
        dependsOn(copy)
        systemProperty("robolectric.offline", "true")
        systemProperty("robolectric.dependency.dir", dir.get().asFile.absolutePath)
    }
}
