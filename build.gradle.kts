import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    `java-gradle-plugin`
    id("com.akuleshov7.vercraft.plugin-gradle") version "0.7.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("me.champeau.jmh") version "0.7.3"
    id("com.saveourtool.diktat") version "2.0.0"
}

group = "io.github.axidex"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.yaml:snakeyaml:2.2")
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

configure<GradlePluginDevelopmentExtension> {
    plugins.create("licenses").apply {
        id = "io.github.axidex.gradle-licenses"
        implementationClass = "io.github.axidex.licenses.LicensesPlugin"
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name = "gradle-licenses"
        description = "Gradle plugin for dependency license compliance checking, inspired by go-bouncer"
        url = "https://github.com/axidex/gradle-licenses"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "axidex"
                name = "axidex"
                url = "https://github.com/axidex"
            }
        }
        scm {
            url = "https://github.com/axidex/gradle-licenses"
            connection = "scm:git:https://github.com/axidex/gradle-licenses.git"
            developerConnection = "scm:git:ssh://git@github.com/axidex/gradle-licenses.git"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Sign only when GPG key is provided (i.e. in CI), skip for local builds
tasks.withType<Sign>().configureEach {
    onlyIf { System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null }
}

tasks.test {
    useJUnitPlatform()
}

diktat {
    reporters {
        plain()
    }
}

tasks.named<Zip>("jmhJar") {
    isZip64 = true
}
