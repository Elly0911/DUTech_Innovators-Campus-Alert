plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.pbdv_project"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.pbdv_project"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
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

        tasks.withType<JavaCompile> {
            options.compilerArgs.add("-Xlint:deprecation")
        }
    }
}

dependencies {

    // Base Android dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.play.services.location)
    implementation(libs.firebase.ui.firestore)

    // UI
    implementation(libs.recyclerview)

    // Google Maps dependencies
    implementation(libs.play.services.maps)
    implementation(libs.maps.android.utils)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Glide for image loading
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    implementation(libs.core.splashscreen)
    implementation(libs.ucrop)

    implementation("com.squareup.picasso:picasso:2.8")
    implementation (libs.okhttp)

    implementation("com.onesignal:OneSignal:[5.1.6, 5.1.99]")

    implementation ("com.github.chrisbanes:PhotoView:2.3.0")

    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation ("com.github.AnyChart:AnyChart-Android:1.1.5")
    implementation ("androidx.core:core-ktx:1.12.0")

}

apply(plugin = "com.google.gms.google-services")