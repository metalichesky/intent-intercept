enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

private val androidCoreVersion = "1.12.0"
private val androidAppCompatVersion = "1.6.1"
private val androidMaterialVersion = "1.10.0"
private val androidPreferenceVersion = "1.2.1"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    this.versionCatalogs {
        this.create("libs") {
            this.library("androidCore", "androidx.core", "core-ktx")
                .version(androidCoreVersion)
            this.library("androidAppCompat", "androidx.appcompat", "appcompat")
                .version(androidAppCompatVersion)
            this.library("androidMaterial", "com.google.android.material", "material")
                .version(androidMaterialVersion)
            this.library("androidPreference", "androidx.preference", "preference-ktx")
                .version(androidPreferenceVersion)
        }
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "intent-intercept"
include(":app")
 