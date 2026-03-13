pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Il plugin Foojay e' opzionale: in Docker/CI alcune reti non raggiungono il Plugin Portal.
// Con JDK 21 gia' presente nell'immagine non serve risoluzione toolchain remota.

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
    "machine-service",
    //"embedded-service",
    "gateway"
)