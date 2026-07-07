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

    fun updateConfig(config: Config) { currentConfig = config }
    fun getCurrentConfig(): Config = currentConfig

    suspend fun chat(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (currentConfig.apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key not set"))
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
                .addHeader("Authorization", "Bearer " + currentConfig.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).optString("error", "Unknown error")
                } catch (e: Exception) {
                    "HTTP " + response.code + ": " + responseBody
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

            Result.failure(Exception("API response format error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ChatMessage(val role: String, val content: String)

    object Prompts {
        fun summarize(text: String) = listOf(
            ChatMessage("system", "Summarize the following text in Chinese concisely."),
            ChatMessage("user", text)
        )
        fun explain(text: String) = listOf(
            ChatMessage("system", "Explain difficult terms and deeper meaning in Chinese."),
            ChatMessage("user", text)
        )
        fun qa(text: String, question: String) = listOf(
            ChatMessage("system", "Answer questions based on the provided text."),
            ChatMessage("user", "Text: " + text + "\n\nQuestion: " + question)
        )
    }
}
