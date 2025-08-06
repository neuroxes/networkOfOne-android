import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.networkofone"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.networkofone"
        minSdk = 26
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

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        viewBinding = true
    }
}
kapt {
    correctErrorTypes = true
}


dependencies {


    // Hilt dependencies
//    implementation(libs.hilt.android)
//    kapt(libs.hilt.android.compiler)


    //Leak Canary:
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-beta01")

    implementation("com.facebook.shimmer:shimmer:0.5.0")

    implementation("com.jsibbold:zoomage:1.3.1")

    implementation("androidx.work:work-runtime-ktx:2.10.2")

    implementation(libs.firebase.firestore)


    implementation(libs.room.common)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    //implementation("com.github.dhaval2404:imagepicker:2.1")

    // location
    implementation(libs.play.services.maps)

    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")


    implementation("androidx.navigation:navigation-fragment-ktx:2.9.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.2")

    //Auth ui
    implementation("com.firebaseui:firebase-ui-auth:7.2.0")


//    glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

// Lottie dependency
    implementation("com.airbnb.android:lottie:6.6.7")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")

    implementation("androidx.fragment:fragment-ktx:1.8.8")

    // QR Code scanner and generator

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Phone Country Picker
    implementation("com.hbb20:ccp:2.7.3")

    //Dots Indicator
    implementation("com.tbuonomo:dotsindicator:4.3")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}