package io.github.axidex.licenses.resolver

object LicenseMatcher {

    /**
     * Checks if a license name matches any of the given patterns.
     * Patterns are treated as regex (e.g. "Apache.*", "BSD.*", "MIT").
     * Matching is case-insensitive and checks if the pattern is found anywhere in the license name.
     */
    fun matches(licenseName: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(licenseName)
        }
    }

    fun firstMatch(licenseName: String, patterns: List<String>): String? {
        return patterns.firstOrNull { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(licenseName)
        }
    }
}
