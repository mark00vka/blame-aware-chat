package com.github.mark00vka.blameawarechat.blame

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitVcs
import git4idea.annotate.GitFileAnnotation

data class BlameLine(val line: String, val shortHash: String?, val subject: String?)

object BlameAnnotator {
    private val log = thisLogger()

    fun annotate(
        project: Project,
        file: VirtualFile,
        startLine: Int,
        text: String,
    ): List<BlameLine> {
        val lines = text.split('\n')
        val blank = lines.map { BlameLine(it, null, null) }
        return try {
            val vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file) as? GitVcs ?: return blank
            val annotation = vcs.annotationProvider.annotate(file) as? GitFileAnnotation ?: return blank
            lines.mapIndexed { i, raw ->
                val docLine = startLine + i
                val rev = runCatching { annotation.getLineRevisionNumber(docLine) }.getOrNull()
                val hash = rev?.asString()?.take(7)
                val subject = rev?.let {
                    runCatching { annotation.getCommitMessage(it) }.getOrNull()
                }?.lineSequence()?.firstOrNull()?.trim()
                BlameLine(raw, hash, subject)
            }
        } catch (t: Throwable) {
            log.info("Blame annotation failed for ${file.path}: ${t.message}")
            blank
        }
    }
}
