package cn.aixcyi.plugin.dunderall.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.jetbrains.python.psi.PyFile

object ActionUtil {
    /**
     * @see <a href="https://peps.python.org/pep-0263/#defining-the-encoding">PEP 263 - Defining the Encoding</a>
     */
    internal val REGEX_ENCODING_DEFINE = "^[ \\t\\f]*#.*?coding[:=][ \\t]*([-_.a-zA-Z0-9]+)".toRegex()
}

fun PsiComment.isShebang(): Boolean {
    return this.text.startsWith("#!")
}

fun PsiComment.isEncodingDefine(): Boolean {
    return ActionUtil.REGEX_ENCODING_DEFINE.containsMatchIn(this.text)
}

/** 获取 [PyFile] 。 */
fun AnActionEvent.getPyFile(): PyFile? = eval {
    this.getData(CommonDataKeys.PSI_FILE) as PyFile
}

/**
 * 获取编辑器。
 *
 * @param evenIfInactive 见 [LangDataKeys.EDITOR_EVEN_IF_INACTIVE]
 * @return 见 [Editor]
 */
fun AnActionEvent.getEditor(evenIfInactive: Boolean = false): Editor? {
    if (!evenIfInactive)
        return this.getData(LangDataKeys.EDITOR)
    return this.getData(LangDataKeys.EDITOR_EVEN_IF_INACTIVE)
}