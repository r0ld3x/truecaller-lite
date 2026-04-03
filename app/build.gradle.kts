plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.r0ld3x.truecaller"
    compileSdk = 35

    val releaseKeystorePath = System.getenv("KEYSTORE_FILE")?.trim().orEmpty()
    val releaseKeystoreFile = if (releaseKeystorePath.isNotEmpty()) file(releaseKeystorePath) else null
    val releaseSigningEnabled =
        releaseKeystoreFile?.exists() == true &&
            !System.getenv("KEYSTORE_PASSWORD").isNullOrBlank() &&
            !System.getenv("KEY_ALIAS").isNullOrBlank() &&
            !System.getenv("KEY_PASSWORD").isNullOrBlank()

    defaultConfig {
        applicationId = "com.r0ld3x.truecaller"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningEnabled) {
                storeFile = releaseKeystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningEnabled) {
                signingConfig = signingConfigs.getByName("release")
            }
            isDebuggable = false
        }

        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.coil)
    implementation(libs.converter.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.retrofit)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}