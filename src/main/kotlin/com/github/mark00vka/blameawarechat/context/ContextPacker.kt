package com.github.mark00vka.blameawarechat.context

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

data class PackedContext(
    val targetFqName: String,
    val targetFile: KtFile,
    val targetFunction: KtNamedFunction,
    val imports: List<String>,
    val siblingSignatures: List<String>,
    val calledSignatures: List<String>,
)

object ContextPacker {
    private const val MAX_CALLED_FUNCTIONS = 15

    fun packAtCaret(project: Project, editor: Editor): PackedContext? {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? KtFile ?: return null
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset) ?: return null
        val targetFn = PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java) ?: return null

        val imports = psiFile.importList?.imports?.mapNotNull { it.text?.trim() }.orEmpty()
        val siblingSignatures = collectSiblingSignatures(targetFn)
        val calledSignatures = collectCalledFunctions(project, targetFn)
        val fqName = buildFqName(targetFn)

        return PackedContext(
            targetFqName = fqName,
            targetFile = psiFile,
            targetFunction = targetFn,
            imports = imports,
            siblingSignatures = siblingSignatures,
            calledSignatures = calledSignatures,
        )
    }

    private fun buildFqName(fn: KtNamedFunction): String {
        val name = fn.name ?: "<anonymous>"
        val container = PsiTreeUtil.getParentOfType(fn, KtClassOrObject::class.java)
        if (container != null) {
            val cfq = container.fqName?.asString()
            if (!cfq.isNullOrEmpty()) return "$cfq.$name"
        }
        val pkg = fn.containingKtFile.packageFqName.asString()
        return if (pkg.isEmpty()) name else "$pkg.$name"
    }

    private fun collectSiblingSignatures(target: KtNamedFunction): List<String> {
        val parent = target.parent ?: return emptyList()
        return parent.children
            .filterIsInstance<KtNamedFunction>()
            .filter { it !== target && it.name != null }
            .map { signatureOf(it) }
    }

    private fun collectCalledFunctions(project: Project, target: KtNamedFunction): List<String> {
        val module = ModuleUtilCore.findModuleForPsiElement(target) ?: return emptyList()
        val scope = module.moduleScope

        val calledNames = PsiTreeUtil.findChildrenOfType(target, KtCallExpression::class.java)
            .mapNotNull { (it.calleeExpression as? KtSimpleNameExpression)?.getReferencedName() }
            .toSet()
        if (calledNames.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        val psiManager = PsiManager.getInstance(project)

        for (vf in FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)) {
            if (result.size >= MAX_CALLED_FUNCTIONS) break
            val ktFile = psiManager.findFile(vf) as? KtFile ?: continue
            for (fn in PsiTreeUtil.findChildrenOfType(ktFile, KtNamedFunction::class.java)) {
                val n = fn.name ?: continue
                if (n !in calledNames || fn === target) continue
                val sig = signatureOf(fn)
                val key = "${ktFile.packageFqName.asString()}::$sig"
                if (seen.add(key)) {
                    result += sig
                    if (result.size >= MAX_CALLED_FUNCTIONS) break
                }
            }
        }
        return result
    }

    private fun signatureOf(fn: KtNamedFunction): String {
        val body = fn.bodyBlockExpression ?: fn.bodyExpression
        val fnText = fn.text ?: return ""
        if (body == null) return fnText.trim()
        val bodyStartInFn = body.textRange.startOffset - fn.textRange.startOffset
        if (bodyStartInFn <= 0 || bodyStartInFn > fnText.length) return fnText.trim()
        return fnText.substring(0, bodyStartInFn)
            .trimEnd()
            .trimEnd('{', '=')
            .trimEnd()
    }
}
