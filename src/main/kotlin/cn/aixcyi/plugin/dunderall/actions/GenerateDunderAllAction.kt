package cn.aixcyi.plugin.dunderall.actions

import cn.aixcyi.plugin.dunderall.I18nProvider.message
import cn.aixcyi.plugin.dunderall.ui.DunderAllGenerator
import cn.aixcyi.plugin.dunderall.utils.DunderAllWrapper
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.inspections.PyEncodingUtil
import com.jetbrains.python.psi.PyExpressionStatement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFromImportStatement
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.aixcyi.utils.ifTrue


/**
 * 在 Python 文件顶部生成 `__all__` 变量。
 *
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
class GenerateDunderAllAction : AnAction() {

    companion object {
        /**
         * @see <a href="https://peps.python.org/pep-0263/#defining-the-encoding">PEP 263 - Defining the Encoding</a>
         * @see [PythonFileType.ENCODING_PATTERN]
         * @see [PyEncodingUtil]
         */
        private val REGEX_ENCODING_DEFINE = "^[ \\t\\f]*#.*?coding[:=][ \\t]*([-_.a-zA-Z0-9]+)".toRegex()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        // 如果不在 Python 文件中则禁用菜单
        event.presentation.isEnabled = event.getData(CommonDataKeys.PSI_FILE) is PyFile
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(LangDataKeys.EDITOR_EVEN_IF_INACTIVE) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? PyFile ?: return

        ReadonlyStatusHandler
            .getInstance(file.project)
            .ensureFilesWritable(listOf(file.virtualFile))
            .hasReadonlyFiles()
            .ifTrue {
                HintManager.getInstance().showErrorHint(editor, message("hint.EditorIsNotWritable.text"))
                return
            }

        // <action id="GenerateDunderAllWithImports">
        val isWithImports = event.actionManager.getId(this)!!.lowercase().contains("import")

        val selector = DunderAllGenerator(file, withImports = isWithImports)
        val symbols = selector.showThenGet() ?: return

        val wrapper = DunderAllWrapper(file)
        val runnable =
            if (wrapper.expression == null) {
                val anchor = properlyPlaceTo(file)
                val snippet = wrapper.express(symbols)
                Runnable { file.addBefore(snippet, anchor) }
            } else {
                Runnable { wrapper.reassign(symbols) }
            }
        WriteCommandAction.runWriteCommandAction(
            file.project,
            message("command.GenerateDunderAll"),
            null,
            runnable,
        )
    }

    /**
     * 确定 `__all__` 变量的位置。
     *
     * @return 变量应该放在哪个元素的前面。
     * @see <a href="https://peps.python.org/pep-0008/#module-level-dunder-names">PEP 8 - 模块级别 Dunder 的布局位置</a>
     */
    private fun properlyPlaceTo(file: PyFile): PsiElement {
        // 这里只考虑完全遵守 PEP 规范的情况，
        // 因为不遵守规范时无法确定确切的位置。
        for (child in file.children) {
            // 跳过文件自身的 docstring
            if (child is PyExpressionStatement && child.expression is PyStringLiteralExpression) {
                continue
            }
            // 跳过 __future__ 导入
            else if (child is PyFromImportStatement && child.isFromFuture) {
                continue
            }
            // 跳过 shebang 和文件编码定义
            else if (child is PsiComment && (child.isShebang() || child.isEncodingDefine())) {
                continue
            }
            // 跳过空格（虽然我也不知道是哪里来的）
            else if (child is PsiWhiteSpace) {
                continue
            }
            // 其它语句都要排在 __all__ 后面
            return child
        }
        return file.firstChild
    }

    private fun PsiComment.isShebang(): Boolean {
        return this.text.startsWith("#!")
    }

    private fun PsiComment.isEncodingDefine(): Boolean {
        return REGEX_ENCODING_DEFINE.containsMatchIn(this.text)
    }
}