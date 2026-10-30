pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val repositoriesTomtomComUsername: String? by extra
val repositoriesTomtomComPassword: String? by extra

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            // Credentials are only needed for the extended flavor of the SDK
            if (repositoriesTomtomComUsername?.isNotEmpty() == true) {
                credentials {
                    username = repositoriesTomtomComUsername
                    password = repositoriesTomtomComPassword
                }
            }
            url = uri("https://repositories.tomtom.com/artifactory/maven")
        }
    }
}

rootProject.name = "tomtomp"
include(":app")
