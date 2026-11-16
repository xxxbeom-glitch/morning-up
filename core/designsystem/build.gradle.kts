plugins {
    alias(libs.plugins.morningup.android.library)
    alias(libs.plugins.morningup.android.compose)
}

android {
    namespace = "com.lumian.morningup.core.designsystem"
}

dependencies {
    api(libs.lucide.icons)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.serialization.json)
}
