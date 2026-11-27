import java.util.Properties

plugins {
    alias(libs.plugins.morningup.android.library)
    alias(libs.plugins.morningup.android.compose)
    alias(libs.plugins.morningup.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// 문의하기 받는 주소(결정 201). git에서 빠진 local.properties의 SUPPORT_EMAIL이나 같은 이름의 환경 변수에서 읽어요.
// 저장소에는 주소를 적지 않아요. 둘 다 없으면 빈 값이고, 문의하기를 누르면 준비 중 안내만 해요.
val supportEmail: String = rootProject.file("local.properties").takeIf { it.isFile }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) }.getProperty("SUPPORT_EMAIL") }
    ?: System.getenv("SUPPORT_EMAIL")
    ?: ""

android {
    namespace = "com.lumian.morningup.feature.settings"

    defaultConfig {
        buildConfigField("String", "SUPPORT_EMAIL", "\"${supportEmail.trim()}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.alarm)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.timber)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
}
