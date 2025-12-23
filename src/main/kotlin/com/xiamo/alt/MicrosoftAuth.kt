package com.xiamo.alt

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xiamo.SuperSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.minecraft.client.MinecraftClient
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * 微软 OAuth 认证
 * 使用 Device Code Flow 实现   打滑水影的
 */
object MicrosoftAuth {

    private val gson = Gson()

    // Azure 应用 Client ID (公开的 Minecraft 启动器 Client ID)
    private const val CLIENT_ID = "00000000402b5328"

    // OAuth 端点
    private const val DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf"
    private const val TOKEN_URL = "https://login.live.com/oauth20_token.srf"
    private const val XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
    private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
    private const val MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
    private const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"


    private fun runOnMainThread(action: () -> Unit) {
        MinecraftClient.getInstance().execute(action)
    }


    suspend fun login(
        onUrl: (String) -> Unit,
        onSuccess: (MinecraftAccount) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val deviceCodeResponse = requestDeviceCode()
                if (deviceCodeResponse == null) {
                    runOnMainThread { onError("无法获取设备代码") }
                    return@withContext
                }

                val userCode = deviceCodeResponse.get("user_code").asString
                val deviceCode = deviceCodeResponse.get("device_code").asString
                val verificationUri = deviceCodeResponse.get("verification_uri").asString
                val interval = deviceCodeResponse.get("interval")?.asInt ?: 5
                val expiresIn = deviceCodeResponse.get("expires_in")?.asInt ?: 900

                val loginUrl = "$verificationUri?otc=$userCode"

                try {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(loginUrl), null)
                } catch (e: Exception) {
                    SuperSoft.logger.warn("Failed to copy to clipboard: ${e.message}")
                }

                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(loginUrl))
                    }
                } catch (e: Exception) {
                    SuperSoft.logger.warn("Failed to open browser: ${e.message}")
                }


                runOnMainThread { onUrl(loginUrl) }

                SuperSoft.logger.info("Microsoft login started. URL: $loginUrl, Code: $userCode")


                val msToken = pollForToken(deviceCode, interval, expiresIn)
                if (msToken == null) {
                    runOnMainThread { onError("登录超时或被取消") }
                    return@withContext
                }

                SuperSoft.logger.info("Got Microsoft token")


                val xblToken = authenticateXboxLive(msToken)
                if (xblToken == null) {
                    runOnMainThread { onError("Xbox Live 认证失败") }
                    return@withContext
                }

                SuperSoft.logger.info("Got Xbox Live token")


                val xstsResponse = authenticateXSTS(xblToken)
                if (xstsResponse == null) {
                    runOnMainThread { onError("XSTS 认证失败") }
                    return@withContext
                }

                val xstsToken = xstsResponse.first
                val userHash = xstsResponse.second

                SuperSoft.logger.info("Got XSTS token")


                val mcToken = authenticateMinecraft(xstsToken, userHash)
                if (mcToken == null) {
                    runOnMainThread { onError("Minecraft 认证失败") }
                    return@withContext
                }

                SuperSoft.logger.info("Got Minecraft token")


                val profile = getMinecraftProfile(mcToken)
                if (profile == null) {
                    runOnMainThread { onError("获取 Minecraft 档案失败，可能没有购买游戏") }
                    return@withContext
                }

                val account = MinecraftAccount(
                    username = profile.first,
                    uuid = profile.second,
                    type = AccountType.MICROSOFT,
                    accessToken = mcToken
                )

                SuperSoft.logger.info("Microsoft login successful: ${account.username}")

                runOnMainThread { onSuccess(account) }

            } catch (e: Exception) {
                SuperSoft.logger.error("Microsoft login failed", e)
                runOnMainThread { onError(e.message ?: "未知错误") }
            }
        }
    }


    private fun requestDeviceCode(): JsonObject? {
        return try {
            val url = URL(DEVICE_CODE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val params = "client_id=$CLIENT_ID&scope=XboxLive.signin%20offline_access&response_type=device_code"

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(params)
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                JsonParser.parseString(response).asJsonObject
            } else {
                SuperSoft.logger.error("Device code request failed: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            SuperSoft.logger.error("Device code request error", e)
            null
        }
    }


    private suspend fun pollForToken(deviceCode: String, interval: Int, expiresIn: Int): String? {
        val startTime = System.currentTimeMillis()
        val timeout = expiresIn * 1000L

        while (System.currentTimeMillis() - startTime < timeout) {
            delay(interval * 1000L)

            try {
                val url = URL(TOKEN_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val params = "client_id=$CLIENT_ID&device_code=$deviceCode&grant_type=urn:ietf:params:oauth:grant-type:device_code"

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(params)
                }

                val response = if (connection.responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }

                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("access_token")) {
                    return json.get("access_token").asString
                }

                val error = json.get("error")?.asString
                when (error) {
                    "authorization_pending" -> continue
                    "slow_down" -> delay(5000)
                    "authorization_declined", "expired_token" -> return null
                }

            } catch (e: Exception) {
                SuperSoft.logger.error("Token polling error", e)
            }
        }

        return null
    }


    private fun authenticateXboxLive(msAccessToken: String): String? {
        return try {
            val url = URL(XBOX_AUTH_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val payload = JsonObject().apply {
                add("Properties", JsonObject().apply {
                    addProperty("AuthMethod", "RPS")
                    addProperty("SiteName", "user.auth.xboxlive.com")
                    addProperty("RpsTicket", "d=$msAccessToken")
                })
                addProperty("RelyingParty", "http://auth.xboxlive.com")
                addProperty("TokenType", "JWT")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(response).asJsonObject
                json.get("Token")?.asString
            } else {
                SuperSoft.logger.error("Xbox Live auth failed: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            SuperSoft.logger.error("Xbox Live auth error", e)
            null
        }
    }


    private fun authenticateXSTS(xblToken: String): Pair<String, String>? {
        return try {
            val url = URL(XSTS_AUTH_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val payload = JsonObject().apply {
                add("Properties", JsonObject().apply {
                    addProperty("SandboxId", "RETAIL")
                    add("UserTokens", com.google.gson.JsonArray().apply {
                        add(xblToken)
                    })
                })
                addProperty("RelyingParty", "rp://api.minecraftservices.com/")
                addProperty("TokenType", "JWT")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(response).asJsonObject
                val token = json.get("Token")?.asString ?: return null
                val userHash = json.getAsJsonObject("DisplayClaims")
                    ?.getAsJsonArray("xui")
                    ?.get(0)?.asJsonObject
                    ?.get("uhs")?.asString ?: return null
                Pair(token, userHash)
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                SuperSoft.logger.error("XSTS auth failed: ${connection.responseCode}, $errorResponse")
                null
            }
        } catch (e: Exception) {
            SuperSoft.logger.error("XSTS auth error", e)
            null
        }
    }


    private fun authenticateMinecraft(xstsToken: String, userHash: String): String? {
        return try {
            val url = URL(MC_AUTH_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val payload = JsonObject().apply {
                addProperty("identityToken", "XBL3.0 x=$userHash;$xstsToken")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(response).asJsonObject
                json.get("access_token")?.asString
            } else {
                SuperSoft.logger.error("Minecraft auth failed: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            SuperSoft.logger.error("Minecraft auth error", e)
            null
        }
    }


    private fun getMinecraftProfile(accessToken: String): Pair<String, String>? {
        return try {
            val url = URL(MC_PROFILE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(response).asJsonObject
                val name = json.get("name")?.asString ?: return null
                val id = json.get("id")?.asString ?: return null
                val uuid = formatUUID(id)
                Pair(name, uuid)
            } else {
                SuperSoft.logger.error("Profile fetch failed: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            SuperSoft.logger.error("Profile fetch error", e)
            null
        }
    }


    private fun formatUUID(uuid: String): String {
        return if (uuid.contains("-")) {
            uuid
        } else {
            "${uuid.substring(0, 8)}-${uuid.substring(8, 12)}-${uuid.substring(12, 16)}-${uuid.substring(16, 20)}-${uuid.substring(20)}"
        }
    }
}
