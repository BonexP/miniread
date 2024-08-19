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
            repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
            repositories {
                google()
                mavenCentral()
                jcenter() // Warning: this repository is going to shut down soon
            }
        }

rootProject.name = "miniread"
include(":app")
