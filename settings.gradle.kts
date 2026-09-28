pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea"))
        maven(url = uri("https://artifact.bytedance.com/repository/pangle"))
        maven(url = uri("https://verve.jfrog.io/artifactory/verve-gradle-release"))
        maven(url = uri("https://artifact.taurusx.com/artifactory/taurusx-sdk/"))
    }
}

rootProject.name = "cloudx-android"

include(":app")
