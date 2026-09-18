package com.balsis.app.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TranscriptionResult(
    val summary: String,
    val fullText: String,
    val detectedContext: String = ""
)

class GeminiTranscriber(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Candidates in priority order
    private val modelCandidates = listOf(
        "gemini-2.0-flash",
        "gemini-1.5-flash-latest",
        "gemini-2.5-flash",
        "gemini-1.5-flash"
    )

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/ogg"
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val jsonPayload = buildRequestPayload(base64Audio, mimeType)
        val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        var lastError: Exception? = null

        for (model in modelCandidates) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val result = parseGeminiResponse(responseBody)
                    return@withContext Result.success(result)
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody).getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    lastError = Exception(errorMsg)
                    // If model not found (404), try next candidate
                    if (response.code != 404 && !errorMsg.contains("not found", ignoreCase = true)) {
                        break
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("Neizdevās sazināties ar Gemini API."))
    }

    private fun buildRequestPayload(base64Audio: String, mimeType: String): JSONObject {
        val root = JSONObject()
        val contents = JSONArray()
        val content = JSONObject()
        val parts = JSONArray()

        // 1. Audio Part
        val inlineData = JSONObject().apply {
            put("mimeType", mimeType)
            put("data", base64Audio)
        }
        val audioPart = JSONObject().apply {
            put("inlineData", inlineData)
        }
        parts.put(audioPart)

        // 2. Prompt Part (Instruction for Latvian Transcription & Summary)
        val promptText = """
            Tu esi eksperts latviešu valodas balss ziņu transkribēšanā un apkopošanā.
            Klausies šo audio failu (WhatsApp balss ziņu) latviešu valodā.
            
            Lūdzu atbildi TIKAI un VIENĪGI kā derīgs JSON objekts sekojošā formātā bez jebkādiem papildu markdown blokiem (neliec ```json):
            {
              "summary": "1-2 īsi teikumi latviski ar galveno būtību (laiks, vieta, jautājums, fakti), izlaižot tukšu runāšanu un liekus vārdus",
              "fullText": "Pilns precīzs transkribētais teksts latviešu valodā ar pareizām garumzīmēm un mīkstinājuma zīmēm",
              "context": "Papildu konteksts (piemēram, vai runātājs nosauc savu vārdu, vai uzrunā kādu konkrētu personu)"
            }
        """.trimIndent()

        val textPart = JSONObject().apply {
            put("text", promptText)
        }
        parts.put(textPart)

        content.put("parts", parts)
        contents.put(content)
        root.put("contents", contents)

        // Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", 0.2)
            put("responseMimeType", "application/json")
        }
        root.put("generationConfig", genConfig)

        return root
    }

    private fun parseGeminiResponse(responseJsonStr: String): TranscriptionResult {
        val root = JSONObject(responseJsonStr)
        val candidates = root.getJSONArray("candidates")
        if (candidates.length() == 0) {
            throw IllegalStateException("Gemini returned empty candidates")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val rawText = parts.getJSONObject(0).getString("text").trim()

        // Clean any accidental markdown backticks
        val cleanJsonStr = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val parsedObj = JSONObject(cleanJsonStr)
            TranscriptionResult(
                summary = parsedObj.optString("summary", "Nav kopsavilkuma"),
                fullText = parsedObj.optString("fullText", cleanJsonStr),
                detectedContext = parsedObj.optString("context", "")
            )
        } catch (e: Exception) {
            TranscriptionResult(
                summary = "Kopsavilkums",
                fullText = cleanJsonStr,
                detectedContext = ""
            )
        }
    }
}
