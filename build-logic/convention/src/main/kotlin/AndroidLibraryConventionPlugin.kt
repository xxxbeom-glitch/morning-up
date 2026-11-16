import com.android.build.api.dsl.LibraryExtension
import com.lumian.morningup.buildlogic.configureCodeQuality
import com.lumian.morningup.buildlogic.configureKotlinAndroid
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
        configureCodeQuality()
    }
}
