plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.royalebot.ai"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.royalebot.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.1"
    }
}
