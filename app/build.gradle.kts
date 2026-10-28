plugins {
    id("com.android.application")
}
android {
    namespace = "id.suaralintas.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.suaralintas.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
