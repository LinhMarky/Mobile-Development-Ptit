plugins {
    alias(libs.plugins.android.application)
}
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    buildFeatures { buildConfig = true }
    namespace = "com.example.homely"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.homely"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        val apiUrl = providers.gradleProperty("HOMELY_API_URL").getOrElse("http://10.0.2.2:8080/api/v1")
        require(apiUrl.matches(Regex("https?://[a-zA-Z0-9.:/_-]+"))) { "Invalid HOMELY_API_URL" }
        buildConfigField("String", "API_URL", "\"$apiUrl\"")
        manifestPlaceholders["allowCleartext"] = "false"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["demoPassword"] = providers.environmentVariable("HOMELY_DEMO_PASSWORD").getOrElse("")
    }

    buildTypes {
        debug { manifestPlaceholders["allowCleartext"] = "true" }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk)
    implementation(libs.okhttp)
    implementation(libs.firebase.messaging)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.json.java)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
