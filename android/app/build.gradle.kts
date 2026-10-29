import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Release signing reads keystore.properties at the project root (never committed; see
// keystore.properties.example). Without it, release builds are produced unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.example.ungdungchiasecaycanh"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.ungdungchiasecaycanh"
        minSdk = 29
        targetSdk = 37
        // Raise versionCode on every build handed to testers or Play.
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            // R8: shrinks, optimizes and obfuscates the release build (rules in proguard-rules.pro).
            optimization {
                enable = true
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)
    // Crash reports (free on the Spark plan); release builds upload the R8 mapping file.
    implementation(libs.firebase.crashlytics)
    // App Check for Firebase AI Logic: Play Integrity in release, a debug provider in debug builds.
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.preference)
    // Sign in with Google through Credential Manager.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    // Facebook Login (button hidden until res/values/facebook.xml has a Meta app ID).
    implementation(libs.facebook.login)
    // Plant Doctor's in-app camera (live preview, framing and photo checks).
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    // Real org.json for DoctorAiParser tests (android.jar only ships stubs).
    testImplementation(libs.org.json)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
