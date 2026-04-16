package com.github.mark00vka.blameawarechat.settings

import com.github.mark00vka.blameawarechat.client.LlmProvider
import com.intellij.ide.util.PropertiesComponent

object LlmSettings {
    private const val PROVIDER_KEY = "BlameAwareChat.provider"
    private const val OLLAMA_URL_KEY = "BlameAwareChat.ollamaBaseUrl"
    private const val DEFAULT_OLLAMA_URL = "http://localhost:11434"

    private val props: PropertiesComponent
        get() = PropertiesComponent.getInstance()

    var provider: LlmProvider
        get() = runCatching {
            LlmProvider.valueOf(props.getValue(PROVIDER_KEY, LlmProvider.GEMINI.name))
        }.getOrDefault(LlmProvider.GEMINI)
        set(value) {
            props.setValue(PROVIDER_KEY, value.name)
        }

    var ollamaBaseUrl: String
        get() = props.getValue(OLLAMA_URL_KEY, DEFAULT_OLLAMA_URL)
        set(value) {
            props.setValue(OLLAMA_URL_KEY, value.ifBlank { DEFAULT_OLLAMA_URL }, DEFAULT_OLLAMA_URL)
        }
}
