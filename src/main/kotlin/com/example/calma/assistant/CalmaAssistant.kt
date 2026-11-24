package com.example.calma.assistant

import com.example.calma.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface AssistantResult {
    data class Reply(val text: String) : AssistantResult
    data class Failure(val message: String) : AssistantResult
}

/**
 * Habla con la API de Groq (compatible con la de OpenAI). La clave viene de local.properties
 * (ver build.gradle.kts). Usa HttpURLConnection y org.json, que ya trae Android.
 */
class CalmaAssistant(private val apiKey: String = BuildConfig.GROQ_API_KEY) {

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    /** Pide la respuesta al historial completo. */
    suspend fun reply(history: List<ChatTurn>): AssistantResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext AssistantResult.Failure(NO_KEY)
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(requestBody(history).toByteArray()) }
            val code = connection.responseCode
            when {
                code == 429 -> AssistantResult.Failure("Demasiados pedidos seguidos. Probá en un rato.")
                code == 401 -> AssistantResult.Failure("La clave de Groq no es válida.")
                code !in 200..299 -> AssistantResult.Failure("El servicio respondió con un error ($code).")
                else -> parseReply(connection.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (e: IOException) {
            AssistantResult.Failure("Sin conexión. Revisá internet y reintentá.")
        } finally {
            connection.disconnect()
        }
    }

    private fun requestBody(history: List<ChatTurn>): String {
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", AssistantPrompts.SYSTEM_PROMPT))
        history.forEach { turn ->
            messages.put(JSONObject().put("role", if (turn.fromUser) "user" else "assistant").put("content", turn.text))
        }
        return JSONObject()
            .put("model", MODEL)
            .put("messages", messages)
            // gpt-oss razona antes de contestar: poco alcanza para una charla corta, y el razonamiento no se devuelve.
            .put("reasoning_effort", "low")
            .put("include_reasoning", false)
            .put("max_completion_tokens", 1000)
            .toString()
    }

    private fun parseReply(body: String): AssistantResult = try {
        val choice = JSONObject(body).getJSONArray("choices").getJSONObject(0)
        val text = choice.getJSONObject("message").optString("content").trim()
        when {
            text.isEmpty() -> AssistantResult.Failure("El asistente no devolvió texto.")
            else -> AssistantResult.Reply(text)
        }
    } catch (e: org.json.JSONException) {
        AssistantResult.Failure("No se entendió la respuesta del servicio.")
    }

    companion object {
        const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        const val MODEL = "openai/gpt-oss-120b"
        const val NO_KEY = "Asistente apagado: falta GROQ_API_KEY en local.properties."
    }
}
