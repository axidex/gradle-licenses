plugins {
    kotlin("jvm") version "2.2.21"
    id("org.example.licenses")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("com.google.guava:guava:33.1.0-jre")
}