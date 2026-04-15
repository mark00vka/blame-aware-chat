package com.github.mark00vka.blameawarechat.client

import com.github.mark00vka.blameawarechat.settings.ApiKeyStore
import com.github.mark00vka.blameawarechat.settings.LlmSettings

interface LlmClient {
    fun ask(prompt: String): String
}

class MissingApiKeyException(val provider: LlmProvider) :
    RuntimeException("No API key set for ${provider.displayName}. Add one in Settings → Tools → Blame-Aware Chat.")

object LlmClientFactory {
    fun forCurrentSettings(): LlmClient {
        val provider = LlmSettings.provider
        return when (provider) {
            LlmProvider.CLAUDE -> {
                val key = ApiKeyStore.get(provider) ?: throw MissingApiKeyException(provider)
                if (key.isBlank()) throw MissingApiKeyException(provider)
                ClaudeClient(key, provider.defaultModel)
            }
            LlmProvider.GEMINI -> {
                val key = ApiKeyStore.get(provider) ?: throw MissingApiKeyException(provider)
                if (key.isBlank()) throw MissingApiKeyException(provider)
                GeminiClient(key, provider.defaultModel)
            }
            LlmProvider.OLLAMA -> OllamaClient(LlmSettings.ollamaBaseUrl, provider.defaultModel)
        }
    }
}

internal object JsonEscape {
    fun string(s: String): String {
        val sb = StringBuilder(s.length + 16)
        sb.append('"')
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                else -> if (c.code < 0x20) sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
