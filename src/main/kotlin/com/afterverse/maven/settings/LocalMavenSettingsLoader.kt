package com.afterverse.maven.settings

import java.io.File
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest

class LocalMavenSettingsLoader(private val extension: MavenSettingsPluginExtension) {

  fun loadSettings(): Settings {
    val settingsBuildingRequest = DefaultSettingsBuildingRequest()
      .setUserSettingsFile(extension.getUserSettingsFile())
      .setGlobalSettingsFile(GLOBAL_SETTINGS_FILE)
      .setSystemProperties(System.getProperties())

    return DefaultSettingsBuilderFactory().newInstance()
      .build(settingsBuildingRequest)
      .effectiveSettings
  }

  companion object {
    val GLOBAL_SETTINGS_FILE = File(System.getenv("M2_HOME"), "conf/settings.xml")
  }
}