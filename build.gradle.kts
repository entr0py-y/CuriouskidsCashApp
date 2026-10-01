// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false // let this be 2.3.4 for support gradle 9
    id("com.google.dagger.hilt.android") version "2.59" apply false
}