import com.android.build.api.variant.FilterConfiguration
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.animation)

            implementation(libs.kotlin.reflect)
        }

        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.accompanist.systemuicontroller)

            implementation(libs.androidx.navigation.runtime)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.navigation.common)

            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.coil.network.cache.control)

            // 颜色选择器 https://github.com/mhssn95/compose-color-picker
            implementation(libs.color.picker)

            // libsu https://github.com/topjohnwu/libsu
            implementation(libs.libsu.core)
            implementation(libs.libsu.service)
            implementation(libs.libsu.nio)

            // Shizuku https://github.com/RikkaApps/Shizuku-API
            implementation(libs.shizuku.api)
            implementation(libs.shizuku.provider)
        }
    }
}

val signName = "tweak-android-signing-config"

android {
    namespace = "io.github.lumkit.tweak"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        val versionCodeFormat = SimpleDateFormat("yyMMddHH")

        applicationId = "io.github.lumkit.tweak"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = versionCodeFormat.format(Date()).toInt().also { println("version code: $it") }
//        versionCode = 1
        versionName = "0.0.5-Alpha"
    }

    signingConfigs {
        create(signName) {
            storeFile = file("./tweak-android.jks")
            storePassword = "git.lumkit"
            keyAlias = "lumkit"
            keyPassword = "git.lumkit"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(signName)
        }

        release {
            isMinifyEnabled = false
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(signName)
        }

        create("release_mini") {
            isMinifyEnabled = true
            isDebuggable = false
            isJniDebuggable = false
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(signName)
        }
    }

    val abiCodes = mapOf(
        "armeabi-v7a" to 1,
        "arm64-v8a" to 2,
        "x86" to 3,
        "x86_64" to 4
    )
    applicationVariants.all {
        val buildType = this.buildType.name
        val variant = this
        outputs.all {
            val name =
                this.filters.find { it.filterType == FilterConfiguration.FilterType.ABI.name }?.identifier
            val baseAbiCode = abiCodes[name]
            if (baseAbiCode != null) {
                //写入cpu架构信息
                variant.buildConfigField("String", "CUP_ABI", "\"${name}\"")
            }
            if (this is ApkVariantOutputImpl) {
                outputFileName = "Tweak-${defaultConfig.versionName}-${defaultConfig.versionCode}-${buildType.uppercase()}.APK".replace("_", "-")
            }
        }
    }

    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets")
            }
            jniLibs.srcDirs("src\\main\\jniLibs")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

