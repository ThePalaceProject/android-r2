dependencies {
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.engine)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kxml)
    implementation(libs.mockito.android)

    //noinspection DuplicatePlatformClasses
    implementation(libs.org.json)

    implementation(libs.r2.shared)
    implementation(libs.r2.streamer)
    implementation(libs.slf4j)
    implementation(libs.xmlpull)
    implementation(libs.logback.classic)

    api(project(":org.librarysimplified.r2.api"))
    api(project(":org.librarysimplified.r2.vanilla"))
}
