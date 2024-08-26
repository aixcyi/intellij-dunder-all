package cn.aixcyi.plugin.dunderall.actions

import cn.aixcyi.plugin.dunderall.I18nProvider.message
import cn.aixcyi.plugin.dunderall.ui.DunderAllOptimizer
import cn.aixcyi.plugin.dunderall.utils.DunderAllWrapper
import cn.aixcyi.plugin.dunderall.utils.TopSymbolsHandler
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.jetbrains.python.psi.PyFile
import net.aixcyi.utils.whenTrue


/**
 * 优化 Python 源码中已经存在的 `__all__` 变量的值。
 *
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
class OptimizeDunderAllAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        // 如果不在 Python 文件中则禁用菜单
        event.presentation.isEnabled = event.getData(CommonDataKeys.PSI_FILE) is PyFile
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(LangDataKeys.EDITOR_EVEN_IF_INACTIVE) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? PyFile ?: return
        val hint = HintManager.getInstance()

        ReadonlyStatusHandler
            .getInstance(file.project)
            .ensureFilesWritable(listOf(file.virtualFile))
            .hasReadonlyFiles()
            .whenTrue {
                hint.showErrorHint(editor, message("hint.EditorIsNotWritable.text"))
                return
            }

        val wrapper = DunderAllWrapper(file)
        if (wrapper.expression == null) {
            hint.showInformationHint(editor, message("hint.DunderAllNotFound.text"))
            return
        }
        if (!wrapper.isValidAssignment()) {
            hint.showInformationHint(editor, message("hint.InvalidDunderAll.text"))
            return
        }

        // 选择优化方式
        val dialog = DunderAllOptimizer()
        if (!dialog.showAndGet()) return

        // 构造优化后的代码
        val handler = TopSymbolsHandler().init(file)
        val symbols = wrapper.exports.toMutableList()
            .apply { sortWith(handler.getSymbolComparator(dialog.state.mySequenceOrder)) }
            .apply { if (dialog.state.isAutoRemoveNonexistence) handler.remove(this) }

        // 写入编辑器并产生一个撤销选项
        WriteCommandAction.runWriteCommandAction(
            file.project,
            message("command.OptimizeDunderAll"),
            null,
            { wrapper.reassign(symbols) }
        )
        hint.showInformationHint(editor, message("hint.DunderAllOptimized.text"))
    }
}