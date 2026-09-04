pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "SyncWave"
include(":app")
include(":core:webrtc")
include(":core:signaling")
include(":core:media")
include(":core:network")
include(":core:ui")
include(":feature:home")
include(":feature:host")
include(":feature:receiver")
include(":feature:room")
include(":feature:audio")
include(":feature:scan")
