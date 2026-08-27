// Project-level build file
// All buildscripts and repositories are moved to settings.gradle.kts per modern spec

plugins {
    // Just tell the project which plugins we'll be using in the modules
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("com.google.devtools.ksp") apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
