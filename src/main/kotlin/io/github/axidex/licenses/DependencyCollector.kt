package io.github.axidex.licenses

import io.github.axidex.licenses.model.DependencyLicense
import io.github.axidex.licenses.resolver.PomLicenseResolver
import org.gradle.api.Project

internal object DependencyCollector {

    fun collect(project: Project): List<DependencyLicense> {
        val seen = mutableSetOf<String>()
        return (sequenceOf(project) + project.subprojects.asSequence())
            .flatMap { it.configurations.asSequence() }
            .filter { it.isCanBeResolved }
            .flatMap { config ->
                try {
                    config.resolvedConfiguration.resolvedArtifacts
                } catch (e: Exception) {
                    project.logger.debug("Skipping configuration '${config.name}': ${e.message}")
                    emptySet()
                }
            }
            .mapNotNull { artifact ->
                val id = artifact.moduleVersion.id
                val key = "${id.group}:${id.name}:${id.version}"
                if (seen.add(key)) {
                    val licenses = PomLicenseResolver.resolve(project, id.group, id.name, id.version)
                    DependencyLicense(id.group, id.name, id.version, licenses)
                } else {
                    null
                }
            }
            .toList()
    }
}
