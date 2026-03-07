import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.2.21"
    `java-gradle-plugin`
    id("com.akuleshov7.vercraft.plugin-gradle") version "0.6.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
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

gradlePlugin {
    plugins {
        create("licenses") {
            id = "io.github.axidex.licenses"
            implementationClass = "io.github.axidex.licenses.LicensesPlugin"
        }
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
    jvmToolchain(23)
}

// Sign only when GPG key is provided (i.e. in CI), skip for local builds
tasks.withType<Sign>().configureEach {
    onlyIf { System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null }
}

tasks.test {
    useJUnitPlatform()
}
