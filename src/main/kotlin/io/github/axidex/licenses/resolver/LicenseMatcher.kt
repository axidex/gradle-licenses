package io.github.axidex.licenses.resolver

object LicenseMatcher {
    /**
     * Checks if a license name matches any of the given patterns.
     * Patterns are treated as regex (e.g. "Apache.*", "BSD.*", "MIT").
     * Matching is case-insensitive and checks if the pattern is found anywhere in the license name.
     *
     * @param licenseName
     * @param patterns
     * @return true if the license name matches any of the given patterns
     */
    fun isMatch(licenseName: String, patterns: List<String>): Boolean = patterns.any { pattern ->
        Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(licenseName)
    }

    /**
     * @param licenseName
     * @param patterns
     * @return the first pattern that matches the license name, or null if none matches
     */
    fun firstMatch(licenseName: String, patterns: List<String>): String? = patterns.firstOrNull { pattern ->
        Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(licenseName)
    }
}
