package io.github.axidex.licenses.model

/**
 * @property name
 * @property url
 */
data class LicenseInfo(
    val name: String,
    val url: String? = null,
)

/**
 * @property group
 * @property artifact
 * @property version
 * @property licenses
 */
data class DependencyLicense(
    val group: String,
    val artifact: String,
    val version: String,
    val licenses: List<LicenseInfo>,
) {
    val coordinate: String = "$group:$artifact"
    val coordinateWithVersion: String = "$group:$artifact:$version"
}
