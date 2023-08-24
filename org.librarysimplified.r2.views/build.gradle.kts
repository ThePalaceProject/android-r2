dependencies {
    implementation(libs.androidx.app.compat)
    implementation(libs.androidx.constraint.layout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.view.pager2)
    implementation(libs.google.material)
    implementation(libs.kotlin.stdlib)
    implementation(libs.nypl.theme)
    implementation(libs.r2.shared)
    implementation(libs.r2.streamer)
    implementation(libs.slf4j)
    implementation(libs.rxjava2.extensions)
    implementation(libs.rxandroid2)

    api(project(":org.librarysimplified.r2.api"))
    api(project(":org.librarysimplified.r2.ui_thread"))
}
