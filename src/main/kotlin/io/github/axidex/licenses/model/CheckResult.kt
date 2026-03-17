package io.github.axidex.licenses.model

/** Represents the outcome of evaluating a single dependency's license. */
sealed class CheckResult {
    abstract val dependency: DependencyLicense

    /**
     * @property dependency
     * @property matchedPattern
     */
    data class Permitted(override val dependency: DependencyLicense, val matchedPattern: String) : CheckResult()

    /**
     * @property dependency
     */
    data class Ignored(override val dependency: DependencyLicense) : CheckResult()

    /**
     * @property dependency
     */
    data class Forbidden(override val dependency: DependencyLicense) : CheckResult()

    /**
     * @property dependency
     */
    data class Unknown(override val dependency: DependencyLicense) : CheckResult()
}
