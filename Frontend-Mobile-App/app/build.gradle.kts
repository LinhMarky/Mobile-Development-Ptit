import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val roomlyApiUrl = providers.gradleProperty("ROOMLY_API_URL")
    .getOrElse("http://10.0.2.2:18092/api/v1/")
    .trim()
val releaseRequested = gradle.startParameter.taskNames.any {
    val task = it.substringAfterLast(":")
    task in listOf("build", "assemble", "bundle") ||
        Regex("(assemble|bundle|package|install).*release.*", RegexOption.IGNORE_CASE).matches(task)
}
if (releaseRequested) {
    val endpoint = URI(roomlyApiUrl)
    require(endpoint.scheme == "https" && !endpoint.host.isNullOrBlank() &&
        endpoint.host !in listOf("localhost", "127.0.0.1", "10.0.2.2", "::1")) {
        "Release cần -PROOMLY_API_URL=https://<máy-chủ>/api/v1/"
    }
}

android {
    namespace = "com.example.roomly"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.roomly"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"${roomlyApiUrl.trimEnd('/')}/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
}
