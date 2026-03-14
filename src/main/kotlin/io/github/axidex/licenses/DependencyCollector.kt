package io.github.axidex.licenses

import io.github.axidex.licenses.model.DependencyLicense
import io.github.axidex.licenses.resolver.PomLicenseResolver
import io.github.axidex.licenses.util.pmap
import org.gradle.api.Project

internal object DependencyCollector {

    fun collect(project: Project): List<DependencyLicense> {
        val seen = mutableSetOf<String>()
        val uniqueIds = (sequenceOf(project) + project.subprojects.asSequence())
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
                if (seen.add(key)) id else null
            }
            .toList()

        // Phase 1: fetch POM files on the Gradle task thread (Gradle API restriction).
        val idsWithPoms = uniqueIds.map { id ->
            id to PomLicenseResolver.fetchPom(project, id.group, id.name, id.version)
        }

        // Phase 2: parse XML in parallel — no Gradle API involved.
        return idsWithPoms.pmap { (id, pom) ->
            val licenses = if (pom != null) PomLicenseResolver.parseLicenses(pom) else emptyList()
            DependencyLicense(id.group, id.name, id.version, licenses)
        }
    }
}
