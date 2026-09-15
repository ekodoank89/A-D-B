pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOSITORIES
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "A-Y-A"
include(":app")
