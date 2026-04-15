package com.github.mark00vka.blameawarechat.client

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ClaudeClient(
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-6",
) : LlmClient {
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    override fun ask(prompt: String): String {
        val body = """{"model":"$model","max_tokens":2048,"messages":[{"role":"user","content":${JsonEscape.string(prompt)}}]}"""
        val req = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .timeout(Duration.ofSeconds(120))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw RuntimeException("Claude API ${resp.statusCode()}: ${resp.body()}")
        }
        val root = JsonParser.parseString(resp.body()).asJsonObject
        val content = root.getAsJsonArray("content") ?: return ""
        val sb = StringBuilder()
        for (el in content) {
            val obj = el.asJsonObject
            if (obj.get("type")?.asString == "text") {
                sb.append(obj.get("text").asString)
            }
        }
        return sb.toString()
    }
}
