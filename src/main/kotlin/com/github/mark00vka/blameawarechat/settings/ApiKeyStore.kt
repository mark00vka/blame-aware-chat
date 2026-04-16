package com.github.mark00vka.blameawarechat.settings

import com.github.mark00vka.blameawarechat.client.LlmProvider
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object ApiKeyStore {
    private fun attributes(provider: LlmProvider) = CredentialAttributes(
        generateServiceName("BlameAwareChat", "apiKey.${provider.name}")
    )

    fun get(provider: LlmProvider): String? =
        PasswordSafe.instance.getPassword(attributes(provider))

    fun set(provider: LlmProvider, key: String?) {
        val attrs = attributes(provider)
        if (key.isNullOrBlank()) {
            PasswordSafe.instance.set(attrs, null)
        } else {
            PasswordSafe.instance.set(attrs, Credentials(provider.name, key))
        }
    }
}
