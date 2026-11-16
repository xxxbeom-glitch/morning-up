import com.android.build.api.dsl.ApplicationExtension
import com.lumian.morningup.buildlogic.configureCodeQuality
import com.lumian.morningup.buildlogic.configureKotlinAndroid
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
        configureCodeQuality()
    }
}
