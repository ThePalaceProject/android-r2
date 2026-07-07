plugins {
    id("org.thepalaceproject.build.aar")
}

dependencies {
    coreLibraryDesugaring(libs.android.desugaring)

    implementation(libs.androidx.annotation)
    implementation(libs.joda.time)
    implementation(libs.kotlin.stdlib)
    implementation(libs.r2.shared)
    implementation(libs.r2.streamer)
    implementation(libs.rxjava2)
    implementation(libs.slf4j)
}
