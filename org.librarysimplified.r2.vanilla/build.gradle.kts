dependencies {
    implementation(project(":org.librarysimplified.r2.api"))
    implementation(project(":org.librarysimplified.r2.ui_thread"))

    implementation(libs.androidx.annotation)
    implementation(libs.google.failureaccess)
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
}
