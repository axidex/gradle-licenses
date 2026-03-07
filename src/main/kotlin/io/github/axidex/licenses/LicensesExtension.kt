package io.github.axidex.licenses

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class LicensesExtension @Inject constructor(objects: ObjectFactory) {
    val configFile: Property<String> = objects.property(String::class.java)
        .convention(".license-policy.yaml")
}
