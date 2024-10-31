dependencies {
    coreLibraryDesugaring(libs.android.desugaring)

    val dependencyObjects = setOf(
        project(":org.librarysimplified.r2.api"),
        project(":org.librarysimplified.r2.vanilla"),

        libs.bytebuddy,
        libs.bytebuddy.agent,
        libs.junit,
        libs.junit.jupiter.api,
        libs.junit.jupiter.api,
        libs.junit.jupiter.engine,
        libs.junit.jupiter.engine,
        libs.junit.jupiter.vintage,
        libs.junit.platform.commons,
        libs.junit.platform.engine,
        libs.kotlin.reflect,
        libs.kotlin.stdlib,
        libs.kotlinx.coroutines,
        libs.kxml,
        libs.logback.classic,
        libs.mockito.android,
        libs.mockito.core,
        libs.mockito.kotlin,
        libs.objenesis,
        libs.opentest,
        libs.org.json,
        libs.ow2,
        libs.ow2.asm,
        libs.ow2.asm.commons,
        libs.ow2.asm.tree,
        libs.r2.shared,
        libs.r2.streamer,
        libs.slf4j,
        libs.xmlpull,
    )

    for (dep in dependencyObjects) {
        implementation(dep)
        testImplementation(dep)
    }
}

afterEvaluate {
    tasks.matching { task -> task.name.contains("UnitTest") }
        .forEach { task -> task.enabled = true }
}
