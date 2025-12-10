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
    // Note: repositoriesMode is NOT set here to allow Kotlin/JS and Kotlin/Wasm
    // plugins to add their required repositories (Node.js, Yarn, etc.) dynamically
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenMPTDemo"
include(":app")
include(":libopenmpt")
include(":shared")
