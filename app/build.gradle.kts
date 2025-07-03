plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ph.nyxsys.vcastplayv2"
    compileSdk = 35

    defaultConfig {
        applicationId = "ph.nyxsys.vcastplayv2"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.xcom.squareup.retrofit2.retrofit3)
    implementation (libs.converter.gson)
    implementation (libs.adapter.rxjava2)

    // For FTP
    implementation (libs.commons.net)
    // FTP security encryption
    implementation (libs.androidx.security.crypto.ktx)

    //retrofit
    implementation(libs.logging.interceptor)

    //For Location
    implementation(libs.play.services.location)

    //For Images
    implementation(libs.glide)


}