package com.github.mark00vka.blameawarechat.client

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class OllamaClient(
    baseUrl: String,
    private val model: String = "qwen2.5-coder:7b",
) : LlmClient {
    private val endpoint = "${baseUrl.trimEnd('/')}/api/generate"
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    override fun ask(prompt: String): String {
        val body = """{"model":"$model","prompt":${JsonEscape.string(prompt)},"stream":false}"""
        val req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(300))
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw RuntimeException("Ollama ${resp.statusCode()}: ${resp.body()}")
        }
        val root = JsonParser.parseString(resp.body()).asJsonObject
        return root.get("response")?.asString.orEmpty()
    }
}
