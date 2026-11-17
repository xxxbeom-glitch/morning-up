plugins {
    alias(libs.plugins.morningup.android.library)
    alias(libs.plugins.morningup.android.compose)
}

android {
    namespace = "com.lumian.morningup.core.ui"
}

// 알람을 화면 글자로 바꾸는 공통 코드(시각, 요일, 몇 시간 몇 분 후, 미션 이름). 여러 화면 모듈이 같이 써요.
dependencies {
    api(projects.core.model)
    api(projects.core.designsystem)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
}
