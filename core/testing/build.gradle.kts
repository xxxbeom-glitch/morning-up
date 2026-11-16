plugins {
    alias(libs.plugins.morningup.android.library)
}

android {
    namespace = "com.lumian.morningup.core.testing"
}

dependencies {
    api(projects.core.data)
    api(libs.junit)
    api(libs.truth)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
}
