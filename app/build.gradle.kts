import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val signingPropertiesFile = rootProject.file("keystore.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) {
        signingPropertiesFile.inputStream().use(::load)
    }
}

fun signingValue(propertyName: String, environmentName: String): String? =
    System.getenv(environmentName)?.takeIf { it.isNotBlank() }
        ?: signingProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = signingValue("storeFile", "TUNWEAVE_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "TUNWEAVE_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "TUNWEAVE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "TUNWEAVE_KEY_PASSWORD")
val releaseSigningValues = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val releaseSigningEnabled = releaseSigningValues.all { it != null }

if (releaseSigningValues.any { it != null } && !releaseSigningEnabled) {
    throw GradleException(
        "Release signing is incomplete. Configure storeFile, storePassword, " +
            "keyAlias, and keyPassword together.",
    )
}

val releaseStoreFile = releaseStoreFilePath?.let(rootProject::file)
if (releaseSigningEnabled && releaseStoreFile?.isFile != true) {
    throw GradleException("Release keystore does not exist: $releaseStoreFile")
}

android {
    namespace = "io.github.theodorelx.tunweave"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.theodorelx.tunweave"
        ndkVersion = "26.1.10909125"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    signingConfigs {
        if (releaseSigningEnabled) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = "JKS"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningEnabled) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/legal-assets"))
    }
}

val copyLegalNotices by tasks.registering(Copy::class) {
    from(rootProject.file("LICENSE"))
    from(rootProject.file("NOTICE"))
    from(rootProject.file("THIRD_PARTY_NOTICES.md"))
    into(layout.buildDirectory.dir("generated/legal-assets"))
}

tasks.named("preBuild").configure {
    dependsOn(copyLegalNotices)
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)

    // Compose + Material 3
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
