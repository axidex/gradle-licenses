package io.github.axidex.licenses.resolver

import io.github.axidex.licenses.model.LicenseInfo
import org.gradle.api.Project
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object PomLicenseResolver {

    fun resolve(project: Project, group: String, artifact: String, version: String): List<LicenseInfo> {
        val pom = fetchPom(project, group, artifact, version) ?: return emptyList()
        return parseLicenses(pom)
    }

    private fun fetchPom(project: Project, group: String, artifact: String, version: String): File? {
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

    private fun parseLicenses(pom: File): List<LicenseInfo> {
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
}
