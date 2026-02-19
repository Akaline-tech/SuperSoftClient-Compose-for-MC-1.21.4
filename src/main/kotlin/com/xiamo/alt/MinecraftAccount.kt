package com.xiamo.alt

import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.session.Session
import java.util.Optional
import java.util.UUID

/**
 * 账号类型枚举
 */
enum class AccountType(val displayName: String) {
    CRACKED("离线账号"),
    MICROSOFT("微软账号"),
    SESSION("Session");

    companion object {
        fun fromString(type: String): AccountType {
            return entries.find { it.name.equals(type, ignoreCase = true) } ?: CRACKED
        }
    }
}

/**
 * Minecraft 账号数据类
 */
data class MinecraftAccount(
    val username: String,
    val uuid: String,
    val type: AccountType,
    val accessToken: String = "",
    var isFavorite: Boolean = false,
    val addedTime: Long = System.currentTimeMillis()
) {
    /**
     * 创建游戏 Session
     */
    fun createSession(): Session {
        return Session(
            username,
            UUID.fromString(uuid),
            accessToken,
            Optional.empty(),
            Optional.empty()
        )
    }

    /**
     * 登录此账号
     */
    fun login(): Boolean {
        return try {
            val session = createSession()
            MinecraftClient.getInstance().session = session
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取头像 URL
     */
    fun getHeadUrl(size: Int = 64): String {
        return "https://minotar.net/avatar/$username/$size"
    }

    /**
     * 序列化为 JSON
     */
    fun toJson(): JsonObject {
        return JsonObject().apply {
            addProperty("username", username)
            addProperty("uuid", uuid)
            addProperty("type", type.name)
            addProperty("accessToken", accessToken)
            addProperty("isFavorite", isFavorite)
            addProperty("addedTime", addedTime)
        }
    }

    companion object {
        /**
         * 从 JSON 反序列化
         */
        fun fromJson(json: JsonObject): MinecraftAccount {
            return MinecraftAccount(
                username = json.get("username").asString,
                uuid = json.get("uuid").asString,
                type = AccountType.fromString(json.get("type").asString),
                accessToken = json.get("accessToken")?.asString ?: "",
                isFavorite = json.get("isFavorite")?.asBoolean ?: false,
                addedTime = json.get("addedTime")?.asLong ?: System.currentTimeMillis()
            )
        }

        /**
         * 创建离线账号
         */
        fun createCracked(username: String): MinecraftAccount {
            // 使用 UUID v3 基于用户名生成 UUID (与 Minecraft 离线模式一致)
            val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray())
            return MinecraftAccount(
                username = username,
                uuid = uuid.toString(),
                type = AccountType.CRACKED
            )
        }

        /**
         * 从当前 Session 创建账号
         */
        fun fromCurrentSession(): MinecraftAccount {
            val session = MinecraftClient.getInstance().session
            return MinecraftAccount(
                username = session.username,
                uuid = session.uuidOrNull?.toString() ?: UUID.randomUUID().toString(),
                type = if (session.accessToken.isBlank()) AccountType.CRACKED else AccountType.MICROSOFT,
                accessToken = session.accessToken
            )
        }
    }
}
