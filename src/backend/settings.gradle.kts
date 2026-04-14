pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}


rootProject.name = "smart-gym-backend"

include(
    "service-discovery",
    "auth-service",
    "analytics-service",
    "area-service",
    "e2e",
    "machine-service",
    "tracking-service",
    "embedded-service",
    "gateway"
)