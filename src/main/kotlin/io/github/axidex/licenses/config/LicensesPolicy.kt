package io.github.axidex.licenses.config

/**
 * @property permit
 * @property forbid
 * @property ignorePackages
 */
data class LicensesPolicy(
    val permit: List<String> = emptyList(),
    val forbid: List<String> = emptyList(),
    val ignorePackages: Set<String> = emptySet(),
) {
    val mode: Mode = when {
        permit.isNotEmpty() -> Mode.ALLOWLIST
        forbid.isNotEmpty() -> Mode.DENYLIST
        else -> Mode.ALLOWLIST
    }
    init {
        require(permit.isEmpty() || forbid.isEmpty()) {
            "Cannot specify both 'permit' and 'forbid' in .license-policy.yaml — use one or the other"
        }
    }

    enum class Mode {
        ALLOWLIST, DENYLIST
    }
}
