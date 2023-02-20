import buildsrc.conventions.utils.asConsumer
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.DOCUMENTATION
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.DocsType.DOCS_TYPE_ATTRIBUTE
import org.gradle.api.attributes.DocsType.SOURCES
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE

plugins {
  buildsrc.conventions.`java-base`
  kotlin("jvm")
}

description =
  "The Dokka base plugin, patched to include a performance fix https://github.com/Kotlin/dokka/issues/2729"

dependencies {
  implementation("org.jetbrains.dokka:dokka-core:1.7.20")
  implementation("org.jsoup:jsoup:1.15.3")
  implementation("org.jetbrains:markdown:0.3.1")
}

val dokkaBasePluginConsumer by configurations.registering {
  asConsumer()
  isVisible = false
  isTransitive = false
  attributes {
    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
    attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
  }
}

val dokkaBasePluginSourcesConsumer by configurations.registering {
  extendsFrom(dokkaBasePluginConsumer.get())
  attributes {
    attribute(CATEGORY_ATTRIBUTE, objects.named(DOCUMENTATION))
    attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
    attribute(DOCS_TYPE_ATTRIBUTE, objects.named(SOURCES))
  }
}

dependencies {
  dokkaBasePluginConsumer.get()("org.jetbrains.dokka:dokka-base:1.7.20")
}

tasks.jar {
  from(dokkaBasePluginConsumer.map {
    zipTree(
      it.incoming.artifactView {}.artifacts.single().file
    )
  }) {
    exclude("**/ParseHtmlEncodedWithNormalisedSpacesKt.class")
  }
}

val sourcesJar by tasks.getting(Jar::class) {
  from(dokkaBasePluginSourcesConsumer.map {
    zipTree(
      it.incoming.artifactView {}.artifacts.single().file
    )
  }) {
    exclude("**/ParseHtmlEncodedWithNormalisedSpaces.kt")
  }
}
