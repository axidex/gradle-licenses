package io.github.axidex.licenses

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class LicensesListTask : DefaultTask() {

    @TaskAction
    fun list() {
        val dependencies = DependencyCollector.collect(project)
        val col1 = 65
        val col2 = 45
        val total = col1 + col2 + 4

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
}
