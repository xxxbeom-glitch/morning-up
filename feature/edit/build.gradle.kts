plugins {
    alias(libs.plugins.morningup.android.library)
    alias(libs.plugins.morningup.android.compose)
    alias(libs.plugins.morningup.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lumian.morningup.feature.edit"
    // 디버그 빌드만 미션 줄에서 Quick Math를 켜는 임시 길(4단계)에 써요.
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.alarm)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.timber)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
}
