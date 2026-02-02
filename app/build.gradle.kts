plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
}
val versionname by extra("dev")
val versioncode by extra("0.3.1")
val versionnumber by extra(30)

android {
    namespace = "com.i.miniread"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.i.miniread"
        minSdk = 25
        targetSdk = 34
        versionCode = versionnumber
        versionName = versioncode

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        versionNameSuffix = versionname
    }
    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            println("Keystore path: $keystoreFile")

            // 只有在环境变量存在时才配置签名
            if (!keystoreFile.isNullOrEmpty() && File(keystoreFile).exists()) {
                storeFile = File(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                println("Release signing config is set up with keystore")
            } else {
                println("Warning: KEYSTORE_FILE not found or not set. Using debug signing for release build.")
            }
        }
    }
    flavorDimensions += "version"

    productFlavors {
        create("standard") {
            dimension = "version"
            applicationIdSuffix = ""
            versionNameSuffix = "-standard"

            // 标准版配置
            buildConfigField("String", "FLAVOR_TYPE", "\"standard\"")
            buildConfigField("boolean", "IS_EINK", "false")

            resValue("string", "app_name", "MiniRead")
        }

        create("eink") {
            dimension = "version"
            applicationIdSuffix = ".eink"
            versionNameSuffix = "-eink"

            // 电子墨水屏版本配置
            buildConfigField("String", "FLAVOR_TYPE", "\"eink\"")
            buildConfigField("boolean", "IS_EINK", "true")

            resValue("string", "app_name", "MiniRead E-Ink")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = true
            isJniDebuggable = true
            isRenderscriptDebuggable = true
            isDefault = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            versionNameSuffix = "alpha"
            }
        getByName("debug") {
            isDebuggable = true
            isJniDebuggable = true
            isRenderscriptDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation(libs.adapter.guava)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.volley)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging)
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
    mavenCentral()
}
