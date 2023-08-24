dependencies {
  api(libs.androidx.app.compat)
  api(libs.androidx.multidex)
  api(libs.kotlin.stdlib)
  api(libs.slf4j)
  api(libs.logback.android)
  implementation(libs.nypl.theme)

  api(project(":org.librarysimplified.r2.api"))
  api(project(":org.librarysimplified.r2.vanilla"))
  api(project(":org.librarysimplified.r2.views"))
}