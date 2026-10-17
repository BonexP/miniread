plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
}
val appVersionName by extra("1.1.0")
val appVersionCode by extra(110)
val debugVersionSuffix by extra("-debug")

val keystorePath = System.getenv("KEYSTORE_FILE")
val hasReleaseKeystore = keystorePath?.let { it.isNotBlank() && File(it).exists() } == true

android {
    namespace = "com.i.miniread"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.i.miniread"
        minSdk = 25
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        create("release") {
            // 只有在环境变量存在且 keystore 文件存在时才配置正式签名
            if (hasReleaseKeystore) {
                storeFile = File(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    flavorDimensions += "version"

    productFlavors {
        create("standard") {
            dimension = "version"
            applicationIdSuffix = ""

            // 标准版配置
            buildConfigField("String", "FLAVOR_TYPE", "\"standard\"")
            buildConfigField("boolean", "IS_EINK", "false")

            resValue("string", "app_name", "MiniRead")
        }

        create("eink") {
            dimension = "version"
            applicationIdSuffix = ".eink"

            // 电子墨水屏版本配置
            buildConfigField("String", "FLAVOR_TYPE", "\"eink\"")
            buildConfigField("boolean", "IS_EINK", "true")

            resValue("string", "app_name", "MiniRead E-Ink")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            versionNameSuffix = debugVersionSuffix
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = true
            isJniDebuggable = true
            isRenderscriptDebuggable = true
            isDefault = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.logging.interceptor)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.security.crypto)
    implementation(libs.adapter.guava)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


}
repositories {
    google()
    mavenCentral()
}
