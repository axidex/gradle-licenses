package io.github.axidex.licenses.model

sealed class CheckResult {
    abstract val dependency: DependencyLicense

    data class Permitted(override val dependency: DependencyLicense, val matchedPattern: String) : CheckResult()
    data class Ignored(override val dependency: DependencyLicense) : CheckResult()
    data class Forbidden(override val dependency: DependencyLicense) : CheckResult()
    data class Unknown(override val dependency: DependencyLicense) : CheckResult()
}
