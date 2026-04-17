package com.github.mark00vka.blameawarechat.toolWindow

import com.github.mark00vka.blameawarechat.client.LlmClientFactory
import com.github.mark00vka.blameawarechat.client.MissingApiKeyException
import com.github.mark00vka.blameawarechat.context.ContextPacker
import com.github.mark00vka.blameawarechat.context.PromptBuilder
import com.github.mark00vka.blameawarechat.settings.LlmSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class ChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ChatPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.root, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}

private class ChatPanel(private val project: Project) {
    val root: JPanel = JPanel(BorderLayout())

    private val targetLabel = JBLabel("Place caret in a Kotlin function, then click Ask")
    private val response = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
    }
    private val question = JBTextField()
    private val askButton = JButton("Ask")
    private val previewButton = JButton("Preview context")

    init {
        val top = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
            add(targetLabel, BorderLayout.CENTER)
        }
        root.add(top, BorderLayout.NORTH)
        root.add(JBScrollPane(response), BorderLayout.CENTER)

        val bottom = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
            add(question)
            add(Box.createVerticalStrut(4))
            val row = JPanel(BorderLayout())
            row.add(previewButton, BorderLayout.WEST)
            row.add(askButton, BorderLayout.EAST)
            add(row)
        }
        root.add(bottom, BorderLayout.SOUTH)

        askButton.addActionListener { runAsk(send = true) }
        previewButton.addActionListener { runAsk(send = false) }
    }

    private fun currentEditor(): Editor? =
        FileEditorManager.getInstance(project).selectedTextEditor

    private fun runAsk(send: Boolean) {
        val editor = currentEditor()
        if (editor == null) {
            response.text = "No active editor."
            return
        }
        val q = question.text.trim()
        if (send && q.isEmpty()) {
            response.text = "Type a question first."
            return
        }

        response.text = if (send) "Packing context and calling ${LlmSettings.provider.displayName}..." else "Packing context..."

        ApplicationManager.getApplication().executeOnPooledThread {
            val prompt: String? = try {
                ReadAction.compute<String?, Throwable> {
                    val ctx = ContextPacker.packAtCaret(project, editor) ?: return@compute null
                    onEdt { targetLabel.text = "Target: ${ctx.targetFqName}" }
                    PromptBuilder.build(project, ctx, q.ifEmpty { "(no question — preview only)" })
                }
            } catch (t: Throwable) {
                onEdt { response.text = "Error packing context: ${t.message}" }
                return@executeOnPooledThread
            }

            if (prompt == null) {
                onEdt { response.text = "No Kotlin function at caret." }
                return@executeOnPooledThread
            }

            if (!send) {
                onEdt { response.text = prompt }
                return@executeOnPooledThread
            }

            try {
                val client = LlmClientFactory.forCurrentSettings()
                val answer = client.ask(prompt)
                onEdt { response.text = answer }
            } catch (e: MissingApiKeyException) {
                onEdt { response.text = e.message ?: "Missing API key." }
            } catch (t: Throwable) {
                onEdt { response.text = "API error: ${t.message}" }
            }
        }
    }

    private fun onEdt(block: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(block)
    }
}
