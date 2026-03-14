package io.github.axidex.licenses.integration

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LicensesPluginTest {

    @TempDir
    lateinit var projectDir: File

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun setup(policy: String, deps: String = DEFAULT_DEPS) {
        projectDir.resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "test-project"""")

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                id("io.github.axidex.licenses")
            }
            repositories { mavenCentral() }
            dependencies {
                $deps
            }
            """.trimIndent(),
        )

        projectDir.resolve(".license-policy.yaml").writeText(policy)
    }

    private fun runner(vararg args: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*args, "--stacktrace")
        .withPluginClasspath()
        .forwardOutput()

    // -------------------------------------------------------------------------
    // licensesCheck — allowlist mode
    // -------------------------------------------------------------------------

    @Test
    fun `licensesCheck passes when all licenses match permit patterns`() {
        setup(
            policy = """
                permit:
                  - Apache.*
                  - MIT.*
                  - BSD.*
                ignore-packages:
                  # net.bytebuddy is injected by Gradle TestKit's pluginClasspath — not a real project dependency
                  - net.bytebuddy:byte-buddy
            """.trimIndent(),
        )

        val result = runner("licensesCheck").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":licensesCheck")?.outcome)
    }

    @Test
    fun `licensesCheck fails when a dependency license is not in permit list`() {
        setup(
            // jackson-databind is Apache-2.0 — not in permit list below
            policy = """
                permit:
                  - MIT.*
                ignore-packages:
                  - net.bytebuddy:byte-buddy
            """.trimIndent(),
        )

        val result = runner("licensesCheck").buildAndFail()

        assertNotEquals(TaskOutcome.SUCCESS, result.task(":licensesCheck")?.outcome)
        assertTrue(result.output.contains("FAIL"), "Expected FAIL in output")
    }

    @Test
    fun `licensesCheck passes when forbidden dependency is in ignore-packages`() {
        setup(
            policy = """
                permit:
                  - MIT.*
                ignore-packages:
                  - net.bytebuddy:byte-buddy
                  - com.fasterxml.jackson.core:jackson-databind
                  - com.fasterxml.jackson.core:jackson-annotations
                  - com.fasterxml.jackson.core:jackson-core
            """.trimIndent(),
        )

        val result = runner("licensesCheck").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":licensesCheck")?.outcome)
        assertTrue(result.output.contains("IGNORED"))
    }

    // -------------------------------------------------------------------------
    // licensesCheck — denylist mode
    // -------------------------------------------------------------------------

    @Test
    fun `licensesCheck passes in denylist mode when no dependency matches forbid`() {
        setup(
            policy = """
                forbid:
                  - GPL.*
                  - AGPL.*
                  - LGPL.*
                ignore-packages:
                  - net.bytebuddy:byte-buddy
            """.trimIndent(),
        )

        val result = runner("licensesCheck").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":licensesCheck")?.outcome)
    }

    @Test
    fun `licensesCheck fails in denylist mode when a dependency matches forbid`() {
        setup(
            policy = """
                forbid:
                  - Apache.*
                ignore-packages:
                  - net.bytebuddy:byte-buddy
            """.trimIndent(),
        )

        val result = runner("licensesCheck").buildAndFail()

        assertNotEquals(TaskOutcome.SUCCESS, result.task(":licensesCheck")?.outcome)
        assertTrue(result.output.contains("FAIL"))
    }

    // -------------------------------------------------------------------------
    // licensesCheck — config validation
    // -------------------------------------------------------------------------

    @Test
    fun `licensesCheck fails when both permit and forbid are specified`() {
        setup(
            policy = """
                permit:
                  - MIT.*
                forbid:
                  - GPL.*
            """.trimIndent(),
        )

        val result = runner("licensesCheck").buildAndFail()

        assertTrue(
            result.output.contains("permit") && result.output.contains("forbid"),
            "Expected error message mentioning permit and forbid",
        )
    }

    @Test
    fun `licensesCheck fails when policy file is missing`() {
        projectDir.resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "test-project"""")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                id("io.github.axidex.licenses")
            }
            repositories { mavenCentral() }
            """.trimIndent(),
        )
        // intentionally no .license-policy.yaml

        val result = runner("licensesCheck").buildAndFail()

        assertNotEquals(TaskOutcome.SUCCESS, result.task(":licensesCheck")?.outcome)
    }

    @Test
    fun `custom policy file path can be configured via extension`() {
        projectDir.resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "test-project"""")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                id("io.github.axidex.licenses")
            }
            repositories { mavenCentral() }
            dependencies {
                ${DEFAULT_DEPS}
            }
            licenses {
                configFile.set("config/my-policy.yaml")
            }
            """.trimIndent(),
        )
        projectDir.resolve("config").mkdirs()
        projectDir.resolve("config/my-policy.yaml").writeText(
            """
            permit:
              - Apache.*
              - MIT.*
              - BSD.*
            ignore-packages:
              - net.bytebuddy:byte-buddy
            """.trimIndent(),
        )

        val result = runner("licensesCheck").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":licensesCheck")?.outcome)
    }

    @Test
    fun `licensesList always succeeds and prints dependency table`() {
        setup(
            policy = "", // licensesList doesn't read policy
        )

        val result = runner("licensesList").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":licensesList")?.outcome)
        assertTrue(result.output.contains("jackson-databind"), "Expected jackson-databind in output")
        assertTrue(result.output.contains("Apache"), "Expected Apache license in output")
    }

    companion object {
        private const val DEFAULT_DEPS =
            """implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")"""
    }
}
