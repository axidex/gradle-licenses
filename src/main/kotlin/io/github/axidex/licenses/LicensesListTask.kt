package io.github.axidex.licenses

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/** Gradle task that prints a table of all dependency licenses. */
abstract class LicensesListTask : DefaultTask() {
    @TaskAction
    fun list() {
        val dependencies = DependencyCollector.collect(project)
        val col1 = COL_PACKAGE
        val col2 = COL_LICENSE
        val total = col1 + col2 + COL_EXTRA

        println()
        println("=".repeat(total))
        println("  Dependency Licenses")
        println("=".repeat(total))
        println()
        println("  ${"Package".padEnd(col1)}License")
        println("  ${"-".repeat(total - 2)}")

        for (dep in dependencies.sortedBy { it.coordinateWithVersion }) {
            val licenseStr = dep.licenses.joinToString(", ") { it.name }.ifEmpty { "(no license info in POM)" }
            println("  ${dep.coordinateWithVersion.padEnd(col1)}$licenseStr")
        }

        println()
        println("  Total: ${dependencies.size} dependencies")
        println()
    }

    companion object {
        private const val COL_EXTRA = 4
        private const val COL_LICENSE = 45
        private const val COL_PACKAGE = 65
    }
}
