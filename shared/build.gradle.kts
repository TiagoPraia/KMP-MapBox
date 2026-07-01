import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    alias(libs.plugins.ktlint)

    alias(libs.plugins.maven.publish)
}

group = "io.github.tiagopraia"
version = "0.3.5"

kotlin {
    js(IR) {
        browser {
            testTask {
                enabled = false
            }
        }
    }

    androidLibrary {
        namespace = "io.github.tiagopraia.kmp.mapbox.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.mapbox.maps.android)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.compose.html)
            implementation(npm("mapbox-gl", "3.9.4"))
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.tiagopraia.kmp.mapbox"
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kmp-mapbox", version.toString())

    pom {
        name = "KMP MapBox library"
        description = "A KMP library based on MapBox Android and JS implementations."
        inceptionYear = "2026"
        url = "https://github.com/TiagoPraia/KMP-MapBox"
        licenses {
            license {
                name = "The MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "TiagoPraia"
                name = "Tiago Praia"
                url = "https://github.com/TiagoPraia"
            }
        }
        scm {
            url = "https://github.com/TiagoPraia/KMP-MapBox"
            connection = "scm:git:git://github.com/TiagoPraia/KMP-MapBox.git"
            developerConnection = "scm:git:ssh://git@github.com:TiagoPraia/KMP-MapBox.git"
        }
    }
}
