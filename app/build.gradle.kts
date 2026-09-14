plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nmoreland.cognitivenexus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nmoreland.cognitivenexus"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.2"
        buildConfigField("String", "SOURCE_REVISION", "\"${providers.environmentVariable("GITHUB_SHA").orNull?.take(12) ?: "local"}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true; buildConfig = true }

    buildTypes {
        debug { applicationIdSuffix = ".mobile"; versionNameSuffix = "-native-backend-v2" }
        release {
            isMinifyEnabled = false
            val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
            val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
            val keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
            val keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
            if (listOf(keystorePath, keystorePassword, keyAlias, keyPassword).all { !it.isNullOrBlank() }) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(keystorePath!!)
                    storePassword = keystorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                }
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    testImplementation("junit:junit:4.13.2")
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
