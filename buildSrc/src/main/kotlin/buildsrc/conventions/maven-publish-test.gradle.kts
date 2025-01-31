package buildsrc.conventions

import buildsrc.utils.asConsumer
import buildsrc.utils.asProvider


/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

Utility for publishing a project to a local Maven directory for use in integration tests.



* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

plugins {
  base
}

abstract class MavenPublishTest(
  val testMavenRepo: Provider<Directory>
) {
  companion object {
    val attribute = Attribute.of("maven-publish-test", String::class.java)
  }
}

val Gradle.rootGradle: Gradle get() = generateSequence(gradle) { it.parent }.last()

val mavenPublishTestExtension = extensions.create<MavenPublishTest>(
  "mavenPublishTest",
  gradle.rootGradle.rootProject.layout.buildDirectory.dir("test-maven-repo"),
)


val publishToTestMavenRepo by tasks.registering {
  group = PublishingPlugin.PUBLISH_TASK_GROUP
  description = "Publishes all Maven publications to the test Maven repository."
}


plugins.withType<MavenPublishPlugin>().all {
  extensions
    .getByType<PublishingExtension>()
    .publications
    .withType<MavenPublication>().all publication@{
      val publicationName = this@publication.name
      val installTaskName = "publish${publicationName.capitalize()}PublicationToTestMavenRepo"

      // Register a publication task for each publication.
      // Use PublishToMavenLocal, because the PublishToMavenRepository task will *always* create
      // a new jar, even if nothing has changed, and append a timestamp, which results in a large
      // directory and tasks are never up-to-date.
      // PublishToMavenLocal does not append a timestamp, so the target directory is smaller, and
      // up-to-date checks work.
      val installTask = tasks.register<PublishToMavenLocal>(installTaskName) {
        description = "Publishes Maven publication '$publicationName' to the test Maven repository."
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        outputs.cacheIf { true }
        publication = this@publication
        val destinationDir = mavenPublishTestExtension.testMavenRepo.get().asFile
        inputs.property("testMavenRepoTempDir", destinationDir.invariantSeparatorsPath)
        doFirst {
          /**
           * `maven.repo.local` will set the destination directory for this [PublishToMavenLocal] task.
           *
           * @see org.gradle.api.internal.artifacts.mvnsettings.DefaultLocalMavenRepositoryLocator.getLocalMavenRepository
           */
          System.setProperty("maven.repo.local", destinationDir.absolutePath)
        }
      }

      publishToTestMavenRepo.configure {
        dependsOn(installTask)
      }

      tasks.check {
        mustRunAfter(installTask)
      }
    }
}


val testMavenPublication by configurations.registering {
  asConsumer()
  isVisible = false
  attributes {
    attribute(MavenPublishTest.attribute, "testMavenRepo")
  }
}

val testMavenPublicationElements by configurations.registering {
  asProvider()
  isVisible = true
  extendsFrom(testMavenPublication.get())
  attributes {
    attribute(MavenPublishTest.attribute, "testMavenRepo")
  }
  outgoing {
    artifact(mavenPublishTestExtension.testMavenRepo) {
      builtBy(publishToTestMavenRepo)
    }
  }
}

dependencies {
  attributesSchema {
    attribute(MavenPublishTest.attribute)
  }
}
