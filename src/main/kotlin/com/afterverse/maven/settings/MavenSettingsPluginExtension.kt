package com.afterverse.maven.settings

import java.io.File
import org.gradle.api.Project

abstract class MavenSettingsPluginExtension(private val project: Project) {

  /**
   * Name of settings file to use. String is evaluated using {@link org.gradle.api.Project#file(java.lang.Object)}.
   * Defaults to $USER_HOME/.m2/settings.xml.
   */
  var userSettingsFileName = System.getProperty("user.home") + "/.m2/settings.xml"

  fun getUserSettingsFile(): File {
    return project.file(userSettingsFileName)
  }
}
