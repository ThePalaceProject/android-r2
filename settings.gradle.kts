pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("$rootDir/org.thepalaceproject.android.platform/build_libraries.toml"))
        }
    }

    /*
     * The set of repositories used to resolve library dependencies. The order is significant!
     */

    repositories {
        mavenLocal()
        mavenCentral()
        google()

        /*
         * Allow access to the Sonatype snapshots repository.
         */

        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }

        /*
         * Allow access to Jitpack. This is used by, for example, Readium.
         */

        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "org.librarysimplified.r2"

include(":org.librarysimplified.r2.api")
include(":org.librarysimplified.r2.demo")
include(":org.librarysimplified.r2.tests")
include(":org.librarysimplified.r2.ui_thread")
include(":org.librarysimplified.r2.vanilla")
include(":org.librarysimplified.r2.views")
