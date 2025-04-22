plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.healthtracker"
    compileSdk = 35

    lint {
        abortOnError = false
        disable += "NotificationPermission"
    }

    defaultConfig {
        applicationId = "com.example.healthtracker"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets")
            }
        }
    }
}

dependencies {

    implementation ("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation ("com.google.code.gson:gson:2.10.1")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.google.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.ui.auth)
    implementation(libs.glide)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.google.code.gson:gson:2.8.8")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation ("com.google.android.material:material:1.12.0")
    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)

    // CircleImageView for circular profile images
    implementation(libs.circleimageview)


}