import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties
import kotlin.apply

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) load(file.inputStream())
    }

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    alias(libs.plugins.build.konfig)
    alias(libs.plugins.ktlint)
}

buildkonfig {
    packageName = "io.github.tiagopraia.kmp.mapbox"

    defaultConfigs {
        buildConfigField(
            STRING,
            "MAPBOX_ACCESS_TOKEN",
            localProperties.getProperty("MAPBOX_ACCESS_TOKEN", ""),
        )
    }
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.kermit)
        }
        jsMain.dependencies {
            implementation(libs.compose.html)
            implementation(npm("mapbox-gl", "3.9.4"))
        }
    }
}
