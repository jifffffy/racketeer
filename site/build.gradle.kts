import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kobweb.application)
    alias(libs.plugins.kobwebx.markdown)
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://us-central1-maven.pkg.dev/varabyte-repos/public")
}

group = "dev.bitspittle.racketeer.site"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        globals.put("version", SimpleDateFormat("yyyyMMdd.kkmm").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date()).toString())

        index {
            description.set("Powered by Kobweb")
        }
    }
}

kotlin {

    configAsKobwebApplication("docrimes")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
                implementation(libs.kobweb.core)
                implementation(libs.kobweb.silk.core)
                implementation(libs.kobweb.silk.icons.fa)
                implementation(project(":model"))
                implementation(project(":scripting"))
            }
        }
    }
}