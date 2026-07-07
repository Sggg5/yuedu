package io.legado.app.ui.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DeepSeekClient {
    private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val MODEL = "deepseek-chat"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    data class Config(
        val apiKey: String = "",
        val temperature: Float = 0.7f,
        val maxTokens: Int = 4096
    )

    private var currentConfig = Config()

    fun updateConfig(config: Config) {
        currentConfig = config
    }

    fun getCurrentConfig(): Config = currentConfig

    suspend fun chat(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (currentConfig.apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key 鏈缃紝璇峰湪璁剧疆涓緭鍏?DeepSeek API Key"))
            }

            val jsonBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
                put("temperature", currentConfig.temperature)
                put("max_tokens", currentConfig.maxTokens)
            }

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer ${currentConfig.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).optString("error", "Unknown error")
                } catch (e: Exception) {
                    "HTTP ${response.code()}: ${response.message()}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                val content = message?.optString("content", "") ?: ""
                return@withContext Result.success(content)
            }

            Result.failure(Exception("API 杩斿洖鏍煎紡寮傚父"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ChatMessage(val role: String, val content: String)

    // Preset prompts
    object Prompts {
        fun summarize(text: String) = listOf(
            ChatMessage("system", "浣犳槸涓€涓笓涓氱殑闃呰鍔╂墜銆傝鐢ㄧ畝娲佺殑涓枃鎬荤粨浠ヤ笅鏂囨湰鐨勬牳蹇冨唴瀹癸紝鍖呮嫭涓昏鎯呰妭鍜屽叧閿俊鎭€?),
            ChatMessage("user", text)
        )

        fun explain(text: String) = listOf(
            ChatMessage("system", "浣犳槸涓€涓笓涓氱殑鏂囧鍒嗘瀽鍔╂墜銆傝瑙ｉ噴浠ヤ笅娈佃惤涓毦鎳傜殑璇嶆眹銆佸彞寮忋€佸吀鏁呭拰娣卞眰鍚箟锛屽府鍔╄鑰呮洿濂藉湴鐞嗚В銆?),
            ChatMessage("user", text)
        )

        fun qa(text: String, question: String) = listOf(
            ChatMessage("system", "浣犳槸涓€涓笓涓氱殑闃呰闂瓟鍔╂墜銆傝鍩轰簬鎻愪緵鐨勬枃鏈唴瀹瑰洖绛旇鑰呯殑闂銆傚鏋滄枃鏈腑娌℃湁鐩稿叧淇℃伅锛岃濡傚疄璇存槑銆?),
            ChatMessage("user", "鏂囨湰鍐呭锛歕n
闂锛歕")
        )
    }
}

