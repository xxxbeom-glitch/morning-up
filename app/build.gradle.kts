plugins {
    alias(libs.plugins.morningup.android.application)
    alias(libs.plugins.morningup.android.compose)
    alias(libs.plugins.morningup.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lumian.morningup"

    defaultConfig {
        applicationId = "com.lumian.morningup"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "ONBOARDING_ONLY", "false")
            buildConfigField("boolean", "MISSION_ONLY", "false")
        }
        // 온보딩만 보는 확인용 앱(11-19 사용자 지시). 디버그 앱 옆에 따로 깔리고, 켤 때마다 온보딩을 처음부터 다 보여요.
        create("onboarding") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".onboarding"
            versionNameSuffix = "-onboarding"
            matchingFallbacks += "debug"
            buildConfigField("boolean", "ONBOARDING_ONLY", "true")
            buildConfigField("boolean", "MISSION_ONLY", "false")
        }
        // 미션만 해 보는 확인용 앱(11-20 사용자 지시). 디버그 앱, 온보딩 앱 옆에 따로 깔리고, 켜면 미션 목록부터 보여요.
        create("mission") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".mission"
            versionNameSuffix = "-mission"
            matchingFallbacks += "debug"
            buildConfigField("boolean", "ONBOARDING_ONLY", "false")
            buildConfigField("boolean", "MISSION_ONLY", "true")
        }
        release {
            buildConfigField("boolean", "ONBOARDING_ONLY", "false")
            buildConfigField("boolean", "MISSION_ONLY", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 출시 서명 키는 아직 없어요. 출시를 정할 때 keystore.properties로 붙여요(dev-plan 0단계).
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.alarm)
    implementation(projects.feature.home)
    implementation(projects.feature.edit)
    implementation(projects.feature.ring)
    implementation(projects.feature.mission)
    implementation(projects.feature.settings)
    implementation(projects.feature.sleep)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.timber)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
}
