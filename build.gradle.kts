import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
    application
}

group = "ca.derekellis.pantograph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("$buildDir/generated/ksp/main/kotlin")
    }

    sourceSets.test {
        kotlin.srcDir("$buildDir/generated/ksp/test/kotlin")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.sqldelight.adapters)
    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinx.serialization.yaml)

    implementation(libs.logback)
    implementation(libs.clikt)

    implementation(libs.kotlin.inject.runtime)
    ksp(libs.kotlin.inject.compiler)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

sqldelight {
    database("PantographDatabase") {
        packageName = "ca.derekellis.pantograph.db"
    }
}
