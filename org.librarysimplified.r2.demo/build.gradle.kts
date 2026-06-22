import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

plugins {
    this.id("org.thepalaceproject.build.apk")
}

dependencies {
    this.coreLibraryDesugaring(libs.android.desugaring)

    this.implementation(this.project(":org.librarysimplified.r2.api"))
    this.implementation(this.project(":org.librarysimplified.r2.ui_thread"))
    this.implementation(this.project(":org.librarysimplified.r2.vanilla"))
    this.implementation(this.project(":org.librarysimplified.r2.views"))

    this.implementation(libs.androidx.activity)
    this.implementation(libs.androidx.activity.ktx)
    this.implementation(libs.androidx.annotation)
    this.implementation(libs.androidx.appcompat)
    this.implementation(libs.androidx.appcompat.resources)
    this.implementation(libs.androidx.arch.core.common)
    this.implementation(libs.androidx.arch.core.runtime)
    this.implementation(libs.androidx.cardview)
    this.implementation(libs.androidx.collection)
    this.implementation(libs.androidx.constraintlayout)
    this.implementation(libs.androidx.constraintlayout.core)
    this.implementation(libs.androidx.constraintlayout.solver)
    this.implementation(libs.androidx.coordinatorlayout)
    this.implementation(libs.androidx.core)
    this.implementation(libs.androidx.core.ktx)
    this.implementation(libs.androidx.core.splashscreen)
    this.implementation(libs.androidx.cursoradapter)
    this.implementation(libs.androidx.customview)
    this.implementation(libs.androidx.customview.poolingcontainer)
    this.implementation(libs.androidx.drawerlayout)
    this.implementation(libs.androidx.emoji2)
    this.implementation(libs.androidx.emoji2.views)
    this.implementation(libs.androidx.emoji2.views.helper)
    this.implementation(libs.androidx.fragment)
    this.implementation(libs.androidx.fragment.ktx)
    this.implementation(libs.androidx.interpolator)
    this.implementation(libs.androidx.lifecycle.common)
    this.implementation(libs.androidx.lifecycle.extensions)
    this.implementation(libs.androidx.lifecycle.livedata)
    this.implementation(libs.androidx.lifecycle.livedata.core)
    this.implementation(libs.androidx.lifecycle.livedata.core.ktx)
    this.implementation(libs.androidx.lifecycle.livedata.ktx)
    this.implementation(libs.androidx.lifecycle.process)
    this.implementation(libs.androidx.lifecycle.runtime)
    this.implementation(libs.androidx.lifecycle.viewmodel)
    this.implementation(libs.androidx.lifecycle.viewmodel.ktx)
    this.implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    this.implementation(libs.androidx.loader)
    this.implementation(libs.androidx.paging.common)
    this.implementation(libs.androidx.paging.common.ktx)
    this.implementation(libs.androidx.paging.runtime)
    this.implementation(libs.androidx.paging.runtime.ktx)
    this.implementation(libs.androidx.preference)
    this.implementation(libs.androidx.preference.ktx)
    this.implementation(libs.androidx.recyclerview)
    this.implementation(libs.androidx.savedstate)
    this.implementation(libs.androidx.startup.runtime)
    this.implementation(libs.androidx.tracing)
    this.implementation(libs.androidx.vectordrawable)
    this.implementation(libs.androidx.vectordrawable.animated)
    this.implementation(libs.androidx.viewpager)
    this.implementation(libs.androidx.viewpager2)
    this.implementation(libs.google.guava)
    this.implementation(libs.google.material)
    this.implementation(libs.joda.time)
    this.implementation(libs.jsoup)
    this.implementation(libs.kotlin.stdlib)
    this.implementation(libs.kotlinx.coroutines)
    this.implementation(libs.kotlinx.coroutines.android)
    this.implementation(libs.kotlinx.coroutines.core.jvm)
    this.implementation(libs.kotlinx.datetime)
    this.implementation(libs.logback.android)
    this.implementation(libs.palace.theme)
    this.implementation(libs.r2.shared)
    this.implementation(libs.r2.streamer)
    this.implementation(libs.reactive.streams)
    this.implementation(libs.rxandroid2)
    this.implementation(libs.rxjava2)
    this.implementation(libs.rxjava2.extensions)
    this.implementation(libs.slf4j)
}

fun calculateVersionCode(): Int {
    val now = LocalDateTime.now(ZoneId.of("UTC"))
    val nowSeconds = now.toEpochSecond(ZoneOffset.UTC)
    // Seconds since 2021-03-15 09:20:00 UTC
    return (nowSeconds - 1615800000).toInt()
}

val versionCodeCalculated =
    calculateVersionCode()

android {
    this.defaultConfig {
        this.versionName = "0.0.0"
        this.versionCode = versionCodeCalculated

        this@android.androidResources {
            this.localeFilters += listOf("en", "de", "es", "fr", "it")
        }
    }
}

androidComponents {
    this.onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("r2demo-${variant.name}.apk")
        }
    }
}
