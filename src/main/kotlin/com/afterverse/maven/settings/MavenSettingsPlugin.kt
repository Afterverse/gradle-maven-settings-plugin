package com.afterverse.maven.settings

import org.apache.maven.settings.Server
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.SettingsBuildingException
import org.gradle.api.GradleScriptException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension

class MavenSettingsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension =
      project.extensions.create(MAVEN_SETTINGS_EXTENSION_NAME, MavenSettingsPluginExtension::class.java, project)

    project.afterEvaluate {
      val settings = loadSettings(extension)
      project.repositories.applyRepoCredentials(settings)
      project.extensions.findByType(PublishingExtension::class.java)?.repositories?.applyRepoCredentials(settings)
    }
  }

  private fun loadSettings(extension: MavenSettingsPluginExtension): Settings {
    val settingsLoader = LocalMavenSettingsLoader(extension)
    return try {
      settingsLoader.loadSettings()
    } catch (e: SettingsBuildingException) {
      throw GradleScriptException("Unable to read local Maven settings.", e)
    }
  }

  private fun RepositoryHandler.applyRepoCredentials(settings: Settings) {
    val mavenRepositories = this.filterIsInstance<MavenArtifactRepository>()

    mavenRepositories.forEach {
      val server = settings.servers.firstOrNull { server -> it.name == server.id }
        ?: return@forEach

      it.addCredentials(server)
    }
  }

  private fun MavenArtifactRepository.addCredentials(server: Server) {
    if (server.username != null && server.password != null) {
      this.credentials {
        it.username = server.username
        it.password = server.password
      }
    }
  }

  companion object {
    private const val MAVEN_SETTINGS_EXTENSION_NAME = "mavenSettings"
  }
}
