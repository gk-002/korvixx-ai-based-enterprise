package com.example.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Gemini-3.5-flash to get answers.
     * Incorporates custom system instructions for enterprise analysis.
     */
    suspend fun getAnalysis(prompt: String, systemInstruction: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not configured in .env or is placeholder.")
            return@withContext "⚠️ **API Key Config Missing**: Please configure your `GEMINI_API_KEY` in the AI Studio Secrets panel.\n\nHere is a local simulated response:\n\n*Korvix AI analysis of your request indicates high visual coherence across your current portal metrics. All system, HR, and schedule nodes are operating within normal corporate thresholds (99.9% uptime).*"
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            
            // Build the standard REST payload manually to ensure 100% compatibility and avoid complex model serialization errors
            val jsonPayload = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                if (systemInstruction.isNotEmpty()) {
                    val systemInstructionObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", systemInstruction)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put("systemInstruction", systemInstructionObj)
                }

                // Add moderate temperature for standard corporate summaries
                val generationConfigObj = JSONObject().apply {
                    put("temperature", 0.3)
                }
                put("generationConfig", generationConfigObj)
            }

            val body = jsonPayload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Unsuccessful response from Gemini API: ${response.code} - $errBody")
                    return@withContext "⚠️ **API Error (${response.code})**: Unable to fetch AI response from Gemini servers. Please check your network and API key limits."
                }

                val responseBody = response.body?.string() ?: return@withContext "No response body from server"
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Empty response from AI")
                        }
                    }
                }
                
                return@withContext "No coherent answer returned from Gemini."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API execution", e)
            return@withContext "⚠️ **Network Error**: ${e.localizedMessage ?: "Connection timed out"}. Please verify your device internet access."
        }
    }
}
