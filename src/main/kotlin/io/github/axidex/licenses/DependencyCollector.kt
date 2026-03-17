package io.github.axidex.licenses

import io.github.axidex.licenses.model.DependencyLicense
import io.github.axidex.licenses.resolver.PomLicenseResolver
import io.github.axidex.licenses.util.pmap
import org.gradle.api.Project

internal object DependencyCollector {
    /**
     * @param project
     * @return list of all unique dependency licenses found across all configurations
     */
    fun collect(project: Project): List<DependencyLicense> {
        val ownCoordinates = project.allprojects.map { "${it.group}:${it.name}" }.toSet()

        val seen: MutableSet<String> = mutableSetOf()
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
                if ("${id.group}:${id.name}" in ownCoordinates) {
                    return@mapNotNull null
                }
                if (seen.add(key)) id else null
            }
            .toList()

        // Phase 1: fetch POM chain on the Gradle task thread (Gradle API restriction).
        // Follows parent POM links so licenses declared in a BOM/parent are found.
        val idsWithPomChains = uniqueIds.map { id ->
            id to PomLicenseResolver.fetchPomChain(project, id.group, id.name, id.version)
        }

        // Phase 2: parse XML in parallel — no Gradle API involved.
        return idsWithPomChains.pmap { (id, pomChain) ->
            val licenses = PomLicenseResolver.parseLicensesFromChain(pomChain)
            DependencyLicense(id.group, id.name, id.version, licenses)
        }
    }
}
