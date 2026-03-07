package io.github.axidex.licenses.config

import org.yaml.snakeyaml.Yaml
import java.io.File

object PolicyLoader {
    fun load(file: File): LicensesPolicy {
        val yaml = Yaml()
        @Suppress("UNCHECKED_CAST")
        val data = file.inputStream().use { yaml.load<Map<String, Any>>(it) } ?: emptyMap<String, Any>()

        @Suppress("UNCHECKED_CAST")
        val permit = (data["permit"] as? List<String>) ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val forbid = (data["forbid"] as? List<String>) ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val ignorePackages = ((data["ignore-packages"] as? List<String>) ?: emptyList()).toSet()

        return LicensesPolicy(
            permit = permit,
            forbid = forbid,
            ignorePackages = ignorePackages,
        )
    }
}
