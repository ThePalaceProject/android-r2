dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlin.coroutines)
    api(libs.kotlin.coroutines.android)
    api(libs.nano.httpd)
    api(libs.nano.httpd.nanolets)
    api(libs.r2.shared)
    api(libs.r2.streamer)

    implementation(libs.androidx.annotation)
    implementation(libs.slf4j)

    api(project(":org.librarysimplified.r2.api"))
    api(project(":org.librarysimplified.r2.ui_thread"))
}

android {
    buildFeatures.buildConfig = true
}
