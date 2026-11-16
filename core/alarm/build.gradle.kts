plugins {
    alias(libs.plugins.morningup.android.library)
    alias(libs.plugins.morningup.hilt)
}

android {
    namespace = "com.lumian.morningup.core.alarm"
}

dependencies {
    api(projects.core.data)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // 울림 흐름 테스트에서 실제 저장소(Room, DataStore)를 그대로 써요.
    testImplementation(libs.androidx.room.ktx)
    testImplementation(libs.androidx.datastore.preferences)
}
