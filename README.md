[![CI](https://github.com/tiagopraia/kmp-mapbox/actions/workflows/ci.yml/badge.svg)](https://github.com/tiagopraia/kmp-mapbox/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.tiagopraia/kmp-mapbox)](https://central.sonatype.com/artifact/io.github.tiagopraia/kmp-mapbox)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-6750A4?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Mapbox Android](https://img.shields.io/badge/Mapbox%20Android-11.9.0-4264FB?logo=mapbox)](https://docs.mapbox.com/android/maps/)
[![Mapbox GL JS](https://img.shields.io/badge/Mapbox%20GL%20JS-3.9.4-4264FB?logo=mapbox)](https://docs.mapbox.com/mapbox-gl-js/)
[![Targets](https://img.shields.io/badge/Targets-Android%20%7C%20JS-8B5CF6)](#)
[![License MIT](https://img.shields.io/badge/License-MIT-3B82F6)](./LICENSE)

KMP-MapBox is a Kotlin Multiplatform library that instantiates the Map from MapBox libraries,
in Android and in Web. It simplifies your life by creating the map for you. You can fully
use overlays in your own project, using Kotlin for Android or HTML for Web.

HTML is needed for Web, because MapBox JS returns a GL, and since it is an HTMLElement, it can't have any
Kotlin components in overlay. If MapBox puts Wasm as target, then HTML will no longer be needed and the UI
will be shared, but for now, not possible. You can still use ComposeViewport though, just no Kotlin Component
as overlay.

VERY IMPORTANT: Always have your version in the newest one, since all the other tend to be "deleted".

# Documentation
This ADR file (Architecture Decision Record) is an arranged and documented list of
decisions done, for code or project structure, to help AI or developers to not make mistakes done
before. [Click here for ADR file](./ADR.md)

# Features

---

Beside creating the map, you can retrieve your position with latitude, longitude and altitude.
The map uses Fine Location, in Android, for user location, which will retrieve the most accurate
value possible.
The map also lets the user lock in its position, and lets the user put the camera in follow direction mode.

# Setup

---

To use the library it is important to have a Mapbox token, that you can get if by creating an account.
This token needs to be passed to the MapWrapper. If you don't want to put it hardcoded, and you are using
both Android and Web, I recommend using library buildKonfig (https://github.com/yshrsmz/BuildKonfig), it's a plugin
that let's build config work for Android and all the other targets.
If you need help configuring it, look at build.gradle.kts (:webApp).

### Importing Library

To set up the library you need to put in your build.gradle:

````kotlin
implementation("io.github.tiagopraia:kmp-mapbox:0.3.6")
````

or if using libs.versions.toml:

-   in libs.version.toml:
    ````
    [versions]
    (...)
    kmp-mapbox-version = 0.3.6
    
    [libraries]
    (...)
    kmp-mapbox = { module = "io.github.tiagopraia:kmp-mapbox", version.ref = "kmp-mapbox-version" }
    ````

-   in build.gradle (depends on project structure):
    ````kotlin
    kotlin {
        sourceSets {
            commonMain.dependencies {
                implementation(libs.kmp.mapbox)
            }
        }
    }
    ````
    OR

    ````kotlin
    dependencies {
        implementation(libs.kmp.mapbox)
    }
    ````
    
### Other important things

Since this library depends on other libraries, you need to have them in your project too.
(These are the versions library has):
-   They don't need to be together, since one is strictly Android and the other is strictly Web
    ````kotlin
    dependencies {
    implementation("org.jetbrains.compose.html:html-core:1.11.1)
    implementation("com.mapbox.maps:android:11.9.0")
    }
    ````
-   This one goes into the build.gradle even if using libs.versions.toml
    ````kotlin
    implementation(npm("mapbox-gl", "3.9.4"))
    ````
-   In settings.gradle.kts:
    ````kotlin
    dependencyResolutionManagement {
        repositories {
            google {
                mavenContent {
                    // Can have others, but chromium with necessary
                    includeGroupAndSubgroups("org.chromium")
                }
            }
            maven {
                url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            }
        }
    }
    ````