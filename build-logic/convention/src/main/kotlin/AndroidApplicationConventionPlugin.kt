import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.lumian.morningup.buildlogic.configureCodeQuality
import com.lumian.morningup.buildlogic.configureKotlinAndroid
import com.lumian.morningup.buildlogic.configureRobolectricOffline
import com.lumian.morningup.buildlogic.libs
import com.lumian.morningup.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig {
                targetSdk = libs.version("targetSdk").toInt()
                versionCode = libs.version("versionCode").toInt()
                versionName = libs.version("versionName")
            }
        }
        // 단위 테스트는 debug 변형에서만 돌려요. release 변형에는 Compose 테스트용 빈 Activity가 없어요.
        extensions.configure<ApplicationAndroidComponentsExtension> {
            beforeVariants(selector().withBuildType("release")) { it.hostTests.forEach { (_, t) -> t.enable = false } }
        }
        configureRobolectricOffline()
        configureCodeQuality()
    }
}
