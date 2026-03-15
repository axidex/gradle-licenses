# gradle-licenses

A Gradle plugin for dependency license compliance checking. Scans all resolved dependencies across your project and submodules, reads license information from POM files (including parent POM inheritance), and verifies them against a policy you define.

Inspired by [go-bouncer](https://github.com/wagoodman/go-bouncer).

## Installation

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    id("io.github.axidex.gradle-licenses") version "<version>"
}
```

## Quick start

Create `.license-policy.yaml` in the root of your project and run:

```
./gradlew licensesCheck
```

If the file does not exist, the task fails with a configuration error.

## Policy file

The policy file is YAML and supports two modes: allowlist and denylist. You cannot mix both in one file.

### Allowlist

Only the listed licenses are permitted. Everything else fails.

```yaml
permit:
  - Apache
  - MIT
  - BSD
  - MPL
  - ISC
```

### Denylist

All licenses are permitted except the listed ones.

```yaml
forbid:
  - GPL
  - AGPL
  - LGPL
```

### Ignoring packages

Packages listed under `ignore-packages` are excluded from the check entirely. The version is not part of the key — any version of the package is ignored.

```yaml
permit:
  - Apache
  - MIT

ignore-packages:
  - com.example:some-internal-library
  - net.bytebuddy:byte-buddy
```

## Pattern matching

Patterns are regular expressions matched case-insensitively against the license name as a substring. You do not need to anchor or wrap them — `Apache` will match `The Apache Software License, Version 2.0`.

Examples:

| Pattern               | Matches                                                                  |
|-----------------------|--------------------------------------------------------------------------|
| `Apache`              | `The Apache Software License, Version 2.0`, `Apache-2.0`                 |
| `MIT`                 | `MIT`, `The MIT License`                                                 |
| `Classpath Exception` | `GNU General Public License, version 2 with the GNU Classpath Exception` |
| `EPL`                 | `Eclipse Public License v2.0`, `EPL-2.0`                                 |

## Tasks

### `licensesCheck`

Checks all dependencies against the policy. Prints only the packages that fail — forbidden or unknown (no license info found in POM). Exits with a non-zero code if any violations are found.

```
./gradlew licensesCheck
```

Output example:

```
==========================================================================================================================
  License Check — mode: allowlist
==========================================================================================================================

  Package                                                          License                                      Status
  ------------------------------------------------------------------------------------------------------------------------
  com.example:some-lib:1.2.3                                       GPL-3.0                                      FAIL
  org.unknown:no-pom-lib:0.1.0                                     (no license info in POM)                     FAIL

  Summary: 312 permitted, 2 ignored, 1 forbidden, 1 unknown
```

### `licensesList`

Prints all resolved dependencies with their licenses. Always succeeds. Useful for building the initial policy or auditing the dependency tree.

```
./gradlew licensesList
```

## Multi-module projects

Apply the plugin to the root project. It automatically collects dependencies from all subprojects. Subproject modules of the same build are excluded from the check.

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.axidex.gradle-licenses") version "<version>"
}
```

## Custom policy file path

```kotlin
licenses {
    configFile.set("config/license-policy.yaml")
}
```
