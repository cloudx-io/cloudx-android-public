plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "io.cloudx.demo.demoapp"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.cloudx.sample"
        versionCode = 7
        versionName = "1.0"
        // AppLovin SDK 13.6.3 requires API 24. This applies only to the demo; the CloudX SDK keeps
        // its own minimum SDK.
        minSdk = 24
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "cloudx-demo-$name-$versionName.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = libs.versions.kotlinJvmTarget.get()
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE"
            )
        }
    }
}

dependencies {
    implementation(libs.cloudx.sdk)
    implementation(libs.cloudx.adapter.applovin)
    implementation(libs.cloudx.adapter.bigo)
    implementation(libs.cloudx.adapter.digitalturbine)
    implementation(libs.cloudx.adapter.googlewaterfall)
    implementation(libs.cloudx.adapter.inmobi)
    implementation(libs.cloudx.adapter.magnite)
    implementation(libs.cloudx.adapter.meta)
    implementation(libs.cloudx.adapter.mintegral)
    implementation(libs.cloudx.adapter.mobilefuse)
    implementation(libs.cloudx.adapter.moloco)
    implementation(libs.cloudx.adapter.pangle)
    implementation(libs.cloudx.adapter.taurusx)
    implementation(libs.cloudx.adapter.unityads)
    implementation(libs.cloudx.adapter.verve)
    implementation(libs.cloudx.adapter.vungle)

    implementation(libs.core.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.recyclerview)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.android)

    // Google UMP (User Messaging Platform) for consent management
    implementation(libs.google.ump)
}
