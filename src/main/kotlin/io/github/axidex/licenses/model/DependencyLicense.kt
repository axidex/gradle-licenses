package io.github.axidex.licenses.model

data class LicenseInfo(
    val name: String,
    val url: String? = null,
)

data class DependencyLicense(
    val group: String,
    val artifact: String,
    val version: String,
    val licenses: List<LicenseInfo>,
) {
    val coordinate: String get() = "$group:$artifact"
    val coordinateWithVersion: String get() = "$group:$artifact:$version"
}
