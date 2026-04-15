package com.github.mark00vka.blameawarechat.client

import com.google.gson.JsonParser
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

class GeminiClient(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash",
) : LlmClient {
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    override fun ask(prompt: String): String {
        val encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$encodedKey"
        val body = """{"contents":[{"parts":[{"text":${JsonEscape.string(prompt)}}]}]}"""
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(120))
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw RuntimeException("Gemini API ${resp.statusCode()}: ${resp.body()}")
        }
        val root = JsonParser.parseString(resp.body()).asJsonObject
        val candidates = root.getAsJsonArray("candidates") ?: return ""
        if (candidates.size() == 0) return ""
        val content = candidates.get(0).asJsonObject.getAsJsonObject("content") ?: return ""
        val parts = content.getAsJsonArray("parts") ?: return ""
        val sb = StringBuilder()
        for (p in parts) {
            p.asJsonObject.get("text")?.asString?.let(sb::append)
        }
        return sb.toString()
    }
}
