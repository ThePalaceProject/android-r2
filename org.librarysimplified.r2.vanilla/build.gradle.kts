dependencies {
    coreLibraryDesugaring(libs.android.desugaring)

    implementation(project(":org.librarysimplified.r2.api"))
    implementation(project(":org.librarysimplified.r2.ui_thread"))

    implementation(libs.androidx.annotation)
    implementation(libs.google.guava)
    implementation(libs.jcip.annotations)
    implementation(libs.joda.time)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.r2.shared)
    implementation(libs.r2.streamer)
    implementation(libs.rxjava2)
    implementation(libs.slf4j)
}

android {
    buildFeatures.buildConfig = true

    defaultConfig {
        val versionName = project.extra["VERSION_NAME"] as String
        buildConfigField("String", "R2_VERSION_NAME", "\"${versionName}\"")
    }
}
