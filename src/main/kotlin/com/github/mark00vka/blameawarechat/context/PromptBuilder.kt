package com.github.mark00vka.blameawarechat.context

import com.github.mark00vka.blameawarechat.blame.BlameAnnotator
import com.github.mark00vka.blameawarechat.blame.BlameLine
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

object PromptBuilder {
    fun build(project: Project, ctx: PackedContext, question: String): String {
        val sb = StringBuilder()
        sb.append("You are helping with a Kotlin function: ").append(ctx.targetFqName).append("\n\n")

        if (ctx.imports.isNotEmpty()) {
            sb.append("// Imports\n")
            ctx.imports.forEach { sb.append(it).append('\n') }
            sb.append('\n')
        }

        if (ctx.siblingSignatures.isNotEmpty()) {
            sb.append("// Sibling signatures (bodies omitted)\n")
            ctx.siblingSignatures.forEach { sb.append(it).append('\n') }
            sb.append('\n')
        }

        if (ctx.calledSignatures.isNotEmpty()) {
            sb.append("// Same-module functions called from target (signatures only)\n")
            ctx.calledSignatures.forEach { sb.append(it).append('\n') }
            sb.append('\n')
        }

        sb.append("// Target function — each run of lines is prefixed by the commit that introduced it.\n")
        sb.append(renderTargetWithBlame(project, ctx))
        sb.append('\n')

        sb.append("## Question\n").append(question).append('\n')
        return sb.toString()
    }

    private fun renderTargetWithBlame(project: Project, ctx: PackedContext): String {
        val fn = ctx.targetFunction
        val document = PsiDocumentManager.getInstance(project).getDocument(ctx.targetFile) ?: return fn.text
        val range = fn.textRange
        val startLine = document.getLineNumber(range.startOffset)
        val text = document.getText(range)
        val vfile = ctx.targetFile.virtualFile
        val blamed: List<BlameLine> = if (vfile != null) {
            BlameAnnotator.annotate(project, vfile, startLine, text)
        } else {
            text.split('\n').map { BlameLine(it, null, null) }
        }

        val out = StringBuilder()
        var lastHash: String? = null
        for (bl in blamed) {
            if (bl.shortHash != null && bl.shortHash != lastHash) {
                val subject = bl.subject?.takeIf { it.isNotEmpty() } ?: "(no message)"
                out.append("// [").append(bl.shortHash).append("] ").append(subject).append('\n')
                lastHash = bl.shortHash
            }
            out.append(bl.line).append('\n')
        }
        return out.toString()
    }
}
