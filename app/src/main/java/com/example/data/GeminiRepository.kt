package com.example.data

import com.example.api.GeminiService

interface GeminiRepository {
    /**
     * Sends user query with system instructions to Gemini API and returns processed answer.
     */
    suspend fun getAssistantResponse(prompt: String, systemInstruction: String): String
}

class GeminiRepositoryImpl : GeminiRepository {
    override suspend fun getAssistantResponse(prompt: String, systemInstruction: String): String {
        return GeminiService.getAnalysis(prompt, systemInstruction)
    }
}
