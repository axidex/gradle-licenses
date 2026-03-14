package io.github.axidex.licenses.integration

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LicensesPluginMultiModuleTest {

    @TempDir
    lateinit var projectDir: File

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun setup(policy: String, subprojectDeps: String = DEFAULT_SUBPROJECT_DEPS) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include("subproject")
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.axidex.licenses")
            }
            repositories { mavenCentral() }
            """.trimIndent(),
        )

        val subprojectDir = projectDir.resolve("subproject").also { it.mkdirs() }
        subprojectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
            }
            repositories { mavenCentral() }
            dependencies {
                $subprojectDeps
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
    // licensesList — multi-module
    // -------------------------------------------------------------------------

    @Test
    fun `licensesList collects dependencies from subprojects`() {
        setup(policy = "")

        val result = runner("licensesList").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":licensesList")?.outcome)
        assertTrue(result.output.contains("jackson-databind"), "Expected jackson-databind from subproject in output")
        assertTrue(result.output.contains("Apache"), "Expected Apache license from subproject in output")
    }

    // -------------------------------------------------------------------------
    // licensesCheck — multi-module, allowlist mode
    // -------------------------------------------------------------------------

    @Test
    fun `licensesCheck passes when subproject dependencies satisfy permit list`() {
        setup(
            policy = """
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
    fun `licensesCheck fails when subproject has dependency violating permit list`() {
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

        assertEquals(TaskOutcome.FAILED, result.task(":licensesCheck")?.outcome)
        assertTrue(result.output.contains("FAIL"), "Expected FAIL in output")
    }

    @Test
    fun `licensesCheck passes when violating subproject dependency is in ignore-packages`() {
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

    companion object {
        private const val DEFAULT_SUBPROJECT_DEPS =
            """implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")"""
    }
}
