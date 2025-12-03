package com.xiamo.utils.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xiamo.SuperSoft
import com.xiamo.module.Module
import com.xiamo.module.ModuleManager
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * 配置管理器
 * 负责保存和读取所有模块的配置
 * Log由ai处理描述不一定准确
 */
object ConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    val mainDir = File("SuperSoftClient").also {
        if (!it.exists()) it.mkdirs()
    }

    private val configFile: File = File(mainDir, "config.json")


    private var loaded = false




    fun init() {
        load()
        Runtime.getRuntime().addShutdownHook(Thread {
            save()
        })
        SuperSoft.logger.info("ConfigManager initialized")
    }


    fun save() {
        try {
            val rootObject = JsonObject()


            ModuleManager.modules.forEach { module ->
                val moduleObject = JsonObject()


                moduleObject.addProperty("enabled", module.enabled)


                moduleObject.addProperty("key", module.key)


                val settingsObject = JsonObject()
                module.settings.forEach { setting ->
                    settingsObject.add(setting.name, setting.toJson())
                }
                moduleObject.add("settings", settingsObject)

                rootObject.add(module.name, moduleObject)
            }


            FileWriter(configFile).use { writer ->
                gson.toJson(rootObject, writer)
            }

            SuperSoft.logger.info("Config saved to ${configFile.absolutePath}")
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to save config: ${e.message}")
            e.printStackTrace()
        }
    }


    fun load() {
        if (!configFile.exists()) {
            SuperSoft.logger.info("Config file not found, using defaults")
            loaded = true
            return
        }

        try {
            FileReader(configFile).use { reader ->
                val rootObject = JsonParser.parseReader(reader).asJsonObject

                ModuleManager.modules.forEach { module ->
                    if (rootObject.has(module.name)) {
                        val moduleObject = rootObject.getAsJsonObject(module.name)


                        if (moduleObject.has("enabled")) {
                            val shouldBeEnabled = moduleObject.get("enabled").asBoolean
                            module.enabled = shouldBeEnabled
                        }


                        if (moduleObject.has("key")) {
                            module.key = moduleObject.get("key").asInt
                        }


                        if (moduleObject.has("settings")) {
                            val settingsObject = moduleObject.getAsJsonObject("settings")
                            module.settings.forEach { setting ->
                                if (settingsObject.has(setting.name)) {
                                    setting.fromJson(settingsObject.get(setting.name))
                                }
                            }
                        }
                    }
                }
            }

            SuperSoft.logger.info("Config loaded from ${configFile.absolutePath}")
            loaded = true
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to load config: ${e.message}")
            e.printStackTrace()
            loaded = true
        }
    }


    fun saveModule(module: Module) {
        save()
    }


    fun resetAll() {
        ModuleManager.modules.forEach { module ->
            module.enabled = false
            module.settings.forEach { it.reset() }
        }
        save()
    }


    fun resetModule(module: Module) {
        module.settings.forEach { it.reset() }
        saveModule(module)
    }


    fun exportConfig(file: File) {
        try {
            configFile.copyTo(file, overwrite = true)
            SuperSoft.logger.info("Config exported to ${file.absolutePath}")
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to export config: ${e.message}")
        }
    }


    fun importConfig(file: File) {
        try {
            if (file.exists()) {
                file.copyTo(configFile, overwrite = true)
                load()
                SuperSoft.logger.info("Config imported from ${file.absolutePath}")
            }
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to import config: ${e.message}")
        }
    }
}