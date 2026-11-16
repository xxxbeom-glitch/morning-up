import com.lumian.morningup.buildlogic.configureCodeQuality
import com.lumian.morningup.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/** 안드로이드에 기대지 않는 순수 코틀린 모듈(`:core:model`). 테스트가 가장 빨라요. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureKotlinJvm()
        configureCodeQuality()
    }
}
