package com.github.mark00vka.blameawarechat.settings

import com.github.mark00vka.blameawarechat.client.LlmProvider
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class LlmSettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private val providerCombo = JComboBox(LlmProvider.values())
    private val keyField = JBPasswordField()
    private val ollamaUrlField = JBTextField()
    private val helpLabel = JBLabel()

    private val uncommittedKeys = mutableMapOf<LlmProvider, String>()
    private var currentProvider: LlmProvider = LlmSettings.provider

    override fun getDisplayName(): String = "Blame-Aware Chat"

    override fun createComponent(): JComponent {
        currentProvider = LlmSettings.provider
        uncommittedKeys.clear()

        providerCombo.selectedItem = currentProvider
        loadFieldsFor(currentProvider)
        ollamaUrlField.text = LlmSettings.ollamaBaseUrl

        providerCombo.addActionListener {
            val newProvider = providerCombo.selectedItem as? LlmProvider ?: return@addActionListener
            if (newProvider == currentProvider) return@addActionListener
            uncommittedKeys[currentProvider] = String(keyField.password)
            currentProvider = newProvider
            loadFieldsFor(newProvider)
        }

        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Provider:"), providerCombo, 1, false)
            .addLabeledComponent(JBLabel("API key:"), keyField, 1, false)
            .addLabeledComponent(JBLabel("Ollama base URL:"), ollamaUrlField, 1, false)
            .addComponent(helpLabel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        panel = form
        return form
    }

    private fun loadFieldsFor(provider: LlmProvider) {
        val stored = uncommittedKeys[provider] ?: ApiKeyStore.get(provider).orEmpty()
        keyField.text = stored
        keyField.isEnabled = provider.needsApiKey
        helpLabel.text = "<html>${provider.helpText}<br>Default model: <code>${provider.defaultModel}</code></html>"
    }

    override fun isModified(): Boolean {
        val selected = providerCombo.selectedItem as? LlmProvider ?: return false
        if (selected != LlmSettings.provider) return true
        if (ollamaUrlField.text != LlmSettings.ollamaBaseUrl) return true
        if (String(keyField.password) != (ApiKeyStore.get(currentProvider).orEmpty())) return true
        for ((provider, edit) in uncommittedKeys) {
            if (edit != ApiKeyStore.get(provider).orEmpty()) return true
        }
        return false
    }

    override fun apply() {
        val selected = providerCombo.selectedItem as? LlmProvider ?: return
        LlmSettings.provider = selected
        LlmSettings.ollamaBaseUrl = ollamaUrlField.text.trim()

        uncommittedKeys[currentProvider] = String(keyField.password)
        for ((provider, edit) in uncommittedKeys) {
            ApiKeyStore.set(provider, edit)
        }
        uncommittedKeys.clear()
    }

    override fun reset() {
        currentProvider = LlmSettings.provider
        uncommittedKeys.clear()
        providerCombo.selectedItem = currentProvider
        loadFieldsFor(currentProvider)
        ollamaUrlField.text = LlmSettings.ollamaBaseUrl
    }

    override fun disposeUIResources() {
        panel = null
    }
}
