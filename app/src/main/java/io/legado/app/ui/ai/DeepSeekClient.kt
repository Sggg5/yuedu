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
                return@withContext Result.failure(Exception("API Key not configured. Please set DeepSeek API Key in settings."))
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
                .addHeader("Authorization", "Bearer ")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).optString("error", "Unknown error")
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
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

            Result.failure(Exception("API returned unexpected format"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ChatMessage(val role: String, val content: String)

    object Prompts {
        fun summarize(text: String) = listOf(
            ChatMessage("system", "You are a professional reading assistant. Summarize the following text concisely in Chinese, including main plot and key information."),
            ChatMessage("user", text)
        )

        fun explain(text: String) = listOf(
            ChatMessage("system", "You are a professional literary analysis assistant. Explain difficult vocabulary, sentence structures, allusions, and deeper meanings in the following passage."),
            ChatMessage("user", text)
        )

        fun qa(text: String, question: String) = listOf(
            ChatMessage("system", "You are a professional reading Q&A assistant. Answer the reader's question based on the provided text. If the text does not contain relevant information, state this honestly."),
            ChatMessage("user", "Text content:\n$text\n\nQuestion:\n$question")
        )
    }
}


