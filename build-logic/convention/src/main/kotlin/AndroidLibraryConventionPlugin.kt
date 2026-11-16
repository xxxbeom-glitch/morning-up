import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.lumian.morningup.buildlogic.configureCodeQuality
import com.lumian.morningup.buildlogic.configureKotlinAndroid
import com.lumian.morningup.buildlogic.configureRobolectricOffline
import com.lumian.morningup.buildlogic.libs
import com.lumian.morningup.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            testOptions.targetSdk = libs.version("targetSdk").toInt()
            lint.targetSdk = libs.version("targetSdk").toInt()
        }
        // 단위 테스트는 debug 변형에서만 돌려요. release 변형에는 Compose 테스트용 빈 Activity가 없어요.
        extensions.configure<LibraryAndroidComponentsExtension> {
            beforeVariants(selector().withBuildType("release")) { it.hostTests.forEach { (_, t) -> t.enable = false } }
        }
        configureRobolectricOffline()
        configureCodeQuality()
    }
}
