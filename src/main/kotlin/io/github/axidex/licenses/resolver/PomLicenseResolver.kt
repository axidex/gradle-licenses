package io.github.axidex.licenses.resolver

import io.github.axidex.licenses.model.LicenseInfo
import org.gradle.api.Project
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object PomLicenseResolver {
    const val MAX_DEPTH = 10

    /**
     * @param project
     * @param group
     * @param artifact
     * @param version
     * @return chain of POM files from the given coordinates up to the first one declaring licenses
     */
    // Must be called from the Gradle task execution thread (Gradle-managed thread).
    // Follows the parent POM chain up to maxDepth levels to find license declarations.
    internal fun fetchPomChain(
        project: Project,
        group: String,
        artifact: String,
        version: String,
    ): List<File> {
        val chain: MutableList<File> = mutableListOf()
        var currentGroup = group
        var currentArtifact = artifact
        var currentVersion = version
        repeat(MAX_DEPTH) {
            val pom = fetchPom(project, currentGroup, currentArtifact, currentVersion) ?: return chain
            chain.add(pom)
            if (parseLicenses(pom).isNotEmpty()) {
                return chain
            }
            val (parentGroup, parentArtifact, parentVersion) = parseParentCoordinates(pom) ?: return chain
            currentGroup = parentGroup
            currentArtifact = parentArtifact
            currentVersion = parentVersion
        }
        return chain
    }

    /**
     * @param project
     * @param group
     * @param artifact
     * @param version
     * @return the resolved POM file, or null if it cannot be fetched
     */
    // Must be called from the Gradle task execution thread (Gradle-managed thread).
    internal fun fetchPom(
        project: Project,
        group: String,
        artifact: String,
        version: String
    ): File? = try {
        val dep = project.dependencies.create("$group:$artifact:$version@pom")
        val config = project.configurations.detachedConfiguration(dep).apply {
            isTransitive = false
        }
        config.resolvedConfiguration.resolvedArtifacts.firstOrNull()
            ?.file
    } catch (e: Exception) {
        project.logger.debug("Failed to fetch POM for $group:$artifact:$version: ${e.message}")
        null
    }

    /**
     * @param poms
     * @return licenses from the first POM in the chain that declares them, or empty list
     */
    // Pure XML parsing — no Gradle API, safe to call from any thread.
    internal fun parseLicensesFromChain(poms: List<File>): List<LicenseInfo> {
        for (pom in poms) {
            val licenses = parseLicenses(pom)
            if (licenses.isNotEmpty()) {
                return licenses
            }
        }
        return emptyList()
    }

    /**
     * @param pom
     * @return licenses declared in the given POM file, or empty list on parse error
     */
    // Pure XML parsing — no Gradle API, safe to call from any thread.
    internal fun parseLicenses(pom: File): List<LicenseInfo> {
        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom)
            doc.documentElement.normalize()
            val nodes = doc.getElementsByTagName("license")
            (0 until nodes.length).mapNotNull { i ->
                val node = nodes.item(i) as? Element ?: return@mapNotNull null
                val name = node.getElementsByTagName("name").item(0)?.textContent
                    ?.trim()
                    ?: return@mapNotNull null
                val url = node.getElementsByTagName("url").item(0)?.textContent
                    ?.trim()
                LicenseInfo(name = name, url = url)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * @param pom
     * @return Triple of (groupId, artifactId, version) for the parent POM, or null if absent
     */
    // Pure XML parsing — no Gradle API, safe to call from any thread.
    internal fun parseParentCoordinates(pom: File): Triple<String, String, String>? {
        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom)
            doc.documentElement.normalize()
            val parent = doc.getElementsByTagName("parent").item(0) as? Element ?: return null
            val parentGroup = parent.getElementsByTagName("groupId")
                .item(0)
                ?.textContent
                ?.trim() ?: return null
            val parentArtifact = parent.getElementsByTagName("artifactId")
                .item(0)
                ?.textContent
                ?.trim() ?: return null
            val parentVersion = parent.getElementsByTagName("version")
                .item(0)
                ?.textContent
                ?.trim() ?: return null
            Triple(parentGroup, parentArtifact, parentVersion)
        } catch (e: Exception) {
            null
        }
    }
}
