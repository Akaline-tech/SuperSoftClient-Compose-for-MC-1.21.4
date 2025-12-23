package com.xiamo.alt

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.xiamo.SuperSoft
import com.xiamo.utils.config.ConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.session.Session
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*


object AltManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    val accounts = mutableListOf<MinecraftAccount>()


    private var initialSession: Session? = null


    var currentAccountIndex: Int = -1
        private set


    private val accountsFile = File(ConfigManager.mainDir, "accounts.json")


    fun init() {

        initialSession = MinecraftClient.getInstance().session
        SuperSoft.logger.info("Initial session saved: ${initialSession?.username}")


        load()
    }


    fun addCrackedAccount(username: String): Result<MinecraftAccount> {
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("用户名不能为空"))
        }

        if (username.length > 16) {
            return Result.failure(IllegalArgumentException("用户名不能超过16个字符"))
        }


        if (accounts.any { it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("账号已存在"))
        }

        val account = MinecraftAccount.createCracked(username)
        accounts.add(account)
        save()

        SuperSoft.logger.info("Added cracked account: $username")
        return Result.success(account)
    }


    fun addSessionAccount(username: String, uuid: String, accessToken: String): Result<MinecraftAccount> {
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("用户名不能为空"))
        }


        if (accounts.any { it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("账号已存在"))
        }

        val account = MinecraftAccount(
            username = username,
            uuid = uuid,
            type = AccountType.SESSION,
            accessToken = accessToken
        )
        accounts.add(account)
        save()

        SuperSoft.logger.info("Added session account: $username")
        return Result.success(account)
    }


    fun addFromCurrentSession(): Result<MinecraftAccount> {
        val current = MinecraftAccount.fromCurrentSession()


        if (accounts.any { it.username.equals(current.username, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("账号已存在"))
        }

        accounts.add(current)
        save()

        SuperSoft.logger.info("Added current session as account: ${current.username}")
        return Result.success(current)
    }


    fun addMicrosoftAccount(account: MinecraftAccount): Result<MinecraftAccount> {

        val existingIndex = accounts.indexOfFirst { it.username.equals(account.username, ignoreCase = true) }

        if (existingIndex >= 0) {

            accounts[existingIndex] = account
            SuperSoft.logger.info("Updated Microsoft account: ${account.username}")
        } else {

            accounts.add(account)
            SuperSoft.logger.info("Added Microsoft account: ${account.username}")
        }

        save()
        return Result.success(account)
    }


    fun login(index: Int): Result<MinecraftAccount> {
        val account = accounts.getOrNull(index)
            ?: return Result.failure(IndexOutOfBoundsException("账号不存在"))

        return login(account)
    }


    fun login(account: MinecraftAccount): Result<MinecraftAccount> {
        return try {
            if (account.login()) {
                currentAccountIndex = accounts.indexOf(account)
                SuperSoft.logger.info("Logged in as: ${account.username}")
                Result.success(account)
            } else {
                Result.failure(Exception("登录失败"))
            }
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to login: ${e.message}")
            Result.failure(e)
        }
    }


    fun loginCracked(username: String): Result<MinecraftAccount> {
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("用户名不能为空"))
        }

        val account = MinecraftAccount.createCracked(username)
        return try {
            if (account.login()) {
                currentAccountIndex = -1
                SuperSoft.logger.info("Direct login as cracked: $username")
                Result.success(account)
            } else {
                Result.failure(Exception("登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun restoreInitialSession(): Boolean {
        val session = initialSession ?: return false
        return try {
            MinecraftClient.getInstance().session = session
            currentAccountIndex = -1
            SuperSoft.logger.info("Restored initial session: ${session.username}")
            true
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to restore initial session: ${e.message}")
            false
        }
    }


    fun removeAccount(index: Int): Result<MinecraftAccount> {
        if (index < 0 || index >= accounts.size) {
            return Result.failure(IndexOutOfBoundsException("账号不存在"))
        }

        val account = accounts.removeAt(index)
        if (currentAccountIndex == index) {
            currentAccountIndex = -1
        } else if (currentAccountIndex > index) {
            currentAccountIndex--
        }
        save()

        SuperSoft.logger.info("Removed account: ${account.username}")
        return Result.success(account)
    }


    fun toggleFavorite(index: Int): Boolean {
        val account = accounts.getOrNull(index) ?: return false
        accounts[index] = account.copy(isFavorite = !account.isFavorite)
        save()
        return true
    }


    fun swapAccounts(index1: Int, index2: Int): Boolean {
        if (index1 < 0 || index1 >= accounts.size || index2 < 0 || index2 >= accounts.size) {
            return false
        }

        val temp = accounts[index1]
        accounts[index1] = accounts[index2]
        accounts[index2] = temp


        when (currentAccountIndex) {
            index1 -> currentAccountIndex = index2
            index2 -> currentAccountIndex = index1
        }

        save()
        return true
    }


    fun getSortedAccounts(): List<MinecraftAccount> {
        return accounts.sortedWith(
            compareByDescending<MinecraftAccount> { it.isFavorite }
                .thenBy { it.addedTime }
        )
    }


    fun save() {
        try {
            val jsonArray = JsonArray()
            accounts.forEach { account ->
                jsonArray.add(account.toJson())
            }

            FileWriter(accountsFile).use { writer ->
                gson.toJson(jsonArray, writer)
            }

            SuperSoft.logger.info("Saved ${accounts.size} accounts")
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to save accounts: ${e.message}")
        }
    }


    fun load() {
        if (!accountsFile.exists()) {
            SuperSoft.logger.info("No accounts file found, starting fresh")
            return
        }

        try {
            FileReader(accountsFile).use { reader ->
                val jsonArray = JsonParser.parseReader(reader).asJsonArray
                accounts.clear()

                jsonArray.forEach { element ->
                    try {
                        val account = MinecraftAccount.fromJson(element.asJsonObject)
                        accounts.add(account)
                    } catch (e: Exception) {
                        SuperSoft.logger.error("Failed to parse account: ${e.message}")
                    }
                }
            }

            SuperSoft.logger.info("Loaded ${accounts.size} accounts")
        } catch (e: Exception) {
            SuperSoft.logger.error("Failed to load accounts: ${e.message}")
        }
    }


    fun getCurrentUsername(): String {
        return MinecraftClient.getInstance().session.username
    }


    fun getCurrentUUID(): String? {
        return MinecraftClient.getInstance().session.uuidOrNull?.toString()
    }
}
