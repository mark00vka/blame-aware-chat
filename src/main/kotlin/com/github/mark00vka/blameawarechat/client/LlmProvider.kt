package com.github.mark00vka.blameawarechat.client

enum class LlmProvider(
    val displayName: String,
    val defaultModel: String,
    val needsApiKey: Boolean,
    val helpText: String,
) {
    CLAUDE(
        displayName = "Claude (Anthropic)",
        defaultModel = "claude-sonnet-4-6",
        needsApiKey = true,
        helpText = "Paid. Get a key at https://console.anthropic.com/ (Billing → API Keys).",
    ),
    GEMINI(
        displayName = "Gemini (Google AI Studio)",
        defaultModel = "gemini-2.5-flash",
        needsApiKey = true,
        helpText = "Free tier, no credit card. Get a key at https://aistudio.google.com/apikey",
    ),
    OLLAMA(
        displayName = "Ollama (local)",
        defaultModel = "qwen2.5-coder:7b",
        needsApiKey = false,
        helpText = "Fully local, no key. Install from https://ollama.com, then: ollama pull qwen2.5-coder:7b",
    );

    override fun toString(): String = displayName
}
