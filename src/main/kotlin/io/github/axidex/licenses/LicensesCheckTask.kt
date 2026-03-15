package io.github.axidex.licenses

import io.github.axidex.licenses.config.LicensesPolicy
import io.github.axidex.licenses.config.PolicyLoader
import io.github.axidex.licenses.model.CheckResult
import io.github.axidex.licenses.model.DependencyLicense
import io.github.axidex.licenses.resolver.LicenseMatcher
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class LicensesCheckTask : DefaultTask() {

    @get:InputFile
    abstract val policyFile: RegularFileProperty

    @TaskAction
    fun check() {
        val policy = PolicyLoader.load(policyFile.get().asFile)
        val dependencies = DependencyCollector.collect(project)
        val results = dependencies.map { dep -> evaluate(dep, policy) }

        printReport(results, policy)

        val failures = results.filter { it is CheckResult.Forbidden || it is CheckResult.Unknown }
        if (failures.isNotEmpty()) {
            throw GradleException(
                "License check failed: ${failures.count { it is CheckResult.Forbidden }} forbidden, " +
                    "${failures.count { it is CheckResult.Unknown }} unknown. " +
                    "Add to 'permit'/'forbid' or 'ignore-packages' in ${policyFile.get().asFile.name}",
            )
        }
    }

    private fun evaluate(dep: DependencyLicense, policy: LicensesPolicy): CheckResult {
        if (dep.coordinate in policy.ignorePackages) return CheckResult.Ignored(dep)

        if (dep.licenses.isEmpty()) return CheckResult.Unknown(dep)

        val licenseNames = dep.licenses.map { it.name }

        return when (policy.mode) {
            LicensesPolicy.Mode.ALLOWLIST -> {
                val matched = licenseNames.firstNotNullOfOrNull { name ->
                    LicenseMatcher.firstMatch(name, policy.permit)
                }
                if (matched != null) CheckResult.Permitted(dep, matched) else CheckResult.Forbidden(dep)
            }
            LicensesPolicy.Mode.DENYLIST -> {
                val forbidden = licenseNames.any { name ->
                    LicenseMatcher.matches(name, policy.forbid)
                }
                if (forbidden) CheckResult.Forbidden(dep) else CheckResult.Permitted(dep, "(not forbidden)")
            }
        }
    }

    private fun printReport(results: List<CheckResult>, policy: LicensesPolicy) {
        val col1 = 65
        val col2 = 45
        val total = col1 + col2 + 12

        println()
        println("=".repeat(total))
        println("  License Check — mode: ${policy.mode.name.lowercase()}")
        println("=".repeat(total))
        println()
        println("  ${"Package".padEnd(col1)}${"License".padEnd(col2)}Status")
        println("  ${"-".repeat(total - 2)}")

        val sorted = results.sortedWith(compareBy({ statusOrder(it) }, { it.dependency.coordinateWithVersion }))
        for (result in sorted) {
            val dep = result.dependency
            val licenseStr = dep.licenses.joinToString(", ") { it.name }.ifEmpty { "(none)" }
            val (status, label) = when (result) {
                is CheckResult.Ignored   -> "IGNORED " to "(ignored)"
                is CheckResult.Forbidden -> "FAIL    " to licenseStr
                is CheckResult.Unknown   -> "FAIL    " to "(no license info in POM)"
                else                     -> continue
            }
            println("  ${dep.coordinateWithVersion.padEnd(col1)}${label.padEnd(col2)}$status")
        }

        println()
        val permitted = results.count { it is CheckResult.Permitted }
        val ignored   = results.count { it is CheckResult.Ignored }
        val forbidden = results.count { it is CheckResult.Forbidden }
        val unknown   = results.count { it is CheckResult.Unknown }
        println("  Summary: $permitted permitted, $ignored ignored, $forbidden forbidden, $unknown unknown")
        println()
    }

    private fun statusOrder(r: CheckResult) = when (r) {
        is CheckResult.Forbidden -> 0
        is CheckResult.Unknown   -> 1
        is CheckResult.Permitted -> 2
        is CheckResult.Ignored   -> 3
    }
}
