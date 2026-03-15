package io.github.axidex.licenses.resolver

import io.github.axidex.licenses.model.LicenseInfo
import org.gradle.api.Project
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object PomLicenseResolver {

    // Must be called from the Gradle task execution thread (Gradle-managed thread).
    // Follows the parent POM chain up to maxDepth levels to find license declarations.
    internal fun fetchPomChain(
        project: Project,
        group: String,
        artifact: String,
        version: String,
        maxDepth: Int = 4,
    ): List<File> {
        val chain = mutableListOf<File>()
        var g = group; var a = artifact; var v = version
        repeat(maxDepth) {
            val pom = fetchPom(project, g, a, v) ?: return chain
            chain.add(pom)
            if (parseLicenses(pom).isNotEmpty()) return chain
            val (pg, pa, pv) = parseParentCoordinates(pom) ?: return chain
            g = pg; a = pa; v = pv
        }
        return chain
    }

    // Must be called from the Gradle task execution thread (Gradle-managed thread).
    internal fun fetchPom(project: Project, group: String, artifact: String, version: String): File? {
        return try {
            val dep = project.dependencies.create("$group:$artifact:$version@pom")
            val config = project.configurations.detachedConfiguration(dep).apply {
                isTransitive = false
            }
            config.resolvedConfiguration.resolvedArtifacts.firstOrNull()?.file
        } catch (e: Exception) {
            project.logger.debug("Failed to fetch POM for $group:$artifact:$version: ${e.message}")
            null
        }
    }

    // Pure XML parsing — no Gradle API, safe to call from any thread.
    internal fun parseLicensesFromChain(poms: List<File>): List<LicenseInfo> {
        for (pom in poms) {
            val licenses = parseLicenses(pom)
            if (licenses.isNotEmpty()) return licenses
        }
        return emptyList()
    }

    // Pure XML parsing — no Gradle API, safe to call from any thread.
    internal fun parseLicenses(pom: File): List<LicenseInfo> {
        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom)
            doc.documentElement.normalize()
            val nodes = doc.getElementsByTagName("license")
            (0 until nodes.length).mapNotNull { i ->
                val node = nodes.item(i) as? Element ?: return@mapNotNull null
                val name = node.getElementsByTagName("name").item(0)?.textContent?.trim()
                    ?: return@mapNotNull null
                val url = node.getElementsByTagName("url").item(0)?.textContent?.trim()
                LicenseInfo(name = name, url = url)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Pure XML parsing — no Gradle API, safe to call from any thread.
    internal fun parseParentCoordinates(pom: File): Triple<String, String, String>? {
        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom)
            doc.documentElement.normalize()
            val parent = doc.getElementsByTagName("parent").item(0) as? Element ?: return null
            val g = parent.getElementsByTagName("groupId").item(0)?.textContent?.trim() ?: return null
            val a = parent.getElementsByTagName("artifactId").item(0)?.textContent?.trim() ?: return null
            val v = parent.getElementsByTagName("version").item(0)?.textContent?.trim() ?: return null
            Triple(g, a, v)
        } catch (e: Exception) {
            null
        }
    }
}
