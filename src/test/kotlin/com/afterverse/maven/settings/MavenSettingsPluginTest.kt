package com.afterverse.maven.settings

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.net.URI
import org.apache.maven.settings.Server
import org.apache.maven.settings.Settings
import org.apache.maven.settings.io.DefaultSettingsWriter
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder

fun Settings.server(config: Server.() -> Unit) {
  this.servers.add(Server().apply(config))
}

fun configureMavenSettings(settingsFile: File, configureClosure: Settings.() -> Unit) {
  val settings = Settings()
  settings.configureClosure()
  DefaultSettingsWriter().write(settingsFile, null, settings)
}

fun Project.applyPlugin(settingsFile: File) {
  this.pluginManager.apply("com.afterverse.maven-settings")
  this.extensions.configure(MavenSettingsPluginExtension::class.java) {
    it.userSettingsFileName = settingsFile.canonicalPath
  }

  (this as DefaultProject).evaluate()
}

class MavenSettingsPluginTest : BehaviorSpec({

  println(Thread.currentThread())

  val settingsDir = File("build/tmp/.m2/")
  val settingsFile = File(settingsDir, "settings.xml")

  Given("an empty maven-settings file") {
    configureMavenSettings(settingsFile) {}

    When("the project contains any maven specification") {
      val repo2Username = "repo-2-user"
      val repo2Password = "repo-2-pass"

      val project = ProjectBuilder.builder().build()
      project.repositories.apply {
        maven {
          it.name = "repo-1"
          it.url = URI("https://nexus.av/repository/repo-1")
        }

        maven { repo ->
          repo.name = "repo-2"
          repo.url = URI("https://nexus.av/repository/repo-2")
          repo.credentials {
            it.username = repo2Username
            it.password = repo2Password
          }
        }
      }

      project.applyPlugin(settingsFile)

      Then("repo-1 should not have been changed") {
        val repo1 = project.repositories.first { it.name == "repo-1" }.shouldNotBeNull()
        repo1.shouldBeInstanceOf<MavenArtifactRepository>()

        repo1.credentials.username.shouldBeNull()
        repo1.credentials.password.shouldBeNull()
      }

      Then("repo-2 should not have been changed") {
        val repo2 = project.repositories.first { it.name == "repo-2" }.shouldNotBeNull()
        repo2.shouldBeInstanceOf<MavenArtifactRepository>()

        repo2.credentials.username.shouldBe(repo2Username)
        repo2.credentials.password.shouldBe(repo2Password)
      }
    }
  }

  Given("a maven-settings file with one server entry") {
    val serverId = "afterverse-nexus"
    val serverUsername = "afterverse-nexus-user"
    val serverPassword = "afterverse-nexus-pass"

    configureMavenSettings(settingsFile) {
      server {
        id = serverId
        username = serverUsername
        password = serverPassword
      }
    }

    When("no entries of the project matches the server id") {
      val repo2Username = "repo-2-user"
      val repo2Password = "repo-2-pass"

      val project = ProjectBuilder.builder().build()
      project.repositories.apply {
        maven {
          it.name = "repo-1"
          it.url = URI("https://nexus.av/repository/repo-1")
        }

        maven { repo ->
          repo.name = "repo-2"
          repo.url = URI("https://nexus.av/repository/repo-2")
          repo.credentials {
            it.username = repo2Username
            it.password = repo2Password
          }
        }
      }

      project.applyPlugin(settingsFile)

      Then("repo-1 should not have been changed") {
        val repo1 = project.repositories.first { it.name == "repo-1" }.shouldNotBeNull()
        repo1.shouldBeInstanceOf<MavenArtifactRepository>()

        repo1.credentials.username.shouldBeNull()
        repo1.credentials.password.shouldBeNull()
      }

      Then("repo-2 should not have been changed") {
        val repo2 = project.repositories.first { it.name == "repo-2" }.shouldNotBeNull()
        repo2.shouldBeInstanceOf<MavenArtifactRepository>()

        repo2.credentials.username.shouldBe(repo2Username)
        repo2.credentials.password.shouldBe(repo2Password)
      }
    }

    When("the project contains one equals to the server id") {
      val repo2Username = "repo-2-user"
      val repo2Password = "repo-2-pass"

      val project = ProjectBuilder.builder().build()
      project.repositories.apply {
        maven {
          it.name = serverId
          it.url = URI("https://nexus.av/repository/repo-1")
        }

        maven { repo ->
          repo.name = "repo-2"
          repo.url = URI("https://nexus.av/repository/repo-2")
          repo.credentials {
            it.username = repo2Username
            it.password = repo2Password
          }
        }
      }

      project.applyPlugin(settingsFile)

      Then("the first repo should have been modified") {
        val repo1 = project.repositories.first { it.name.startsWith(serverId) }.shouldNotBeNull()
        repo1.shouldBeInstanceOf<MavenArtifactRepository>()

        repo1.credentials.username.shouldBe(serverUsername)
        repo1.credentials.password.shouldBe(serverPassword)
      }

      Then("repo-2 should not have been changed") {
        val repo2 = project.repositories.first { it.name == "repo-2" }.shouldNotBeNull()
        repo2.shouldBeInstanceOf<MavenArtifactRepository>()

        repo2.credentials.username.shouldBe(repo2Username)
        repo2.credentials.password.shouldBe(repo2Password)
      }
    }

    When("the project contains multiple repos with name equal to the server id") {
      val project = ProjectBuilder.builder().build()
      project.repositories.apply {
        maven {
          it.name = serverId
          it.url = URI("https://nexus.av/repository/repo-1")
        }

        maven {
          it.name = serverId
          it.url = URI("https://nexus.av/repository/repo-2")
        }
      }

      project.applyPlugin(settingsFile)

      Then("the first repo should have been modified") {
        val repo1 = project.repositories[0].shouldNotBeNull()
        repo1.shouldBeInstanceOf<MavenArtifactRepository>()

        repo1.credentials.username.shouldBe(serverUsername)
        repo1.credentials.password.shouldBe(serverPassword)
      }

      Then("the second repo should have been modified") {
        val repo2 = project.repositories[1].shouldNotBeNull()
        repo2.shouldBeInstanceOf<MavenArtifactRepository>()

        repo2.credentials.username.shouldBe(serverUsername)
        repo2.credentials.password.shouldBe(serverPassword)
      }
    }

    When("the project contains a match, but it already has any credentials") {
      val repo1Username = "repo-1"
      val repo2Password = "repo-2"

      val project = ProjectBuilder.builder().build()
      project.repositories.apply {
        maven {
          it.name = serverId
          it.url = URI("https://nexus.av/repository/repo-1")
          it.credentials { credentials ->
            credentials.username = repo1Username
          }
        }

        maven {
          it.name = serverId
          it.url = URI("https://nexus.av/repository/repo-2")
          it.credentials { credentials ->
            credentials.password = repo2Password
          }
        }
      }

      project.applyPlugin(settingsFile)

      Then("the first repo should not have been modified") {
        val repo1 = project.repositories[0].shouldNotBeNull()
        repo1.shouldBeInstanceOf<MavenArtifactRepository>()

        repo1.credentials.username.shouldBe(repo1Username)
        repo1.credentials.password.shouldBeNull()
      }

      Then("the second repo should have been modified") {
        val repo2 = project.repositories[1].shouldNotBeNull()
        repo2.shouldBeInstanceOf<MavenArtifactRepository>()

        repo2.credentials.username.shouldBeNull()
        repo2.credentials.password.shouldBe(repo2Password)
      }
    }
  }
})
