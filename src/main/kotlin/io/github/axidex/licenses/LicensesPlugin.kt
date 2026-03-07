package io.github.axidex.licenses

import org.gradle.api.Plugin
import org.gradle.api.Project

class LicensesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("licenses", LicensesExtension::class.java)

        project.tasks.register("licensesCheck", LicensesCheckTask::class.java) { task ->
            task.group = "verification"
            task.description = "Checks dependency licenses against .license-policy.yaml"
            task.policyFile.convention(
                extension.configFile.map { project.layout.projectDirectory.file(it) },
            )
        }

        project.tasks.register("licensesList", LicensesListTask::class.java) { task ->
            task.group = "reporting"
            task.description = "Lists all dependency licenses"
        }
    }
}
