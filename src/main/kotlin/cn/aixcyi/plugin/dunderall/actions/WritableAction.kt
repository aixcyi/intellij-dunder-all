package cn.aixcyi.plugin.dunderall.actions

import cn.aixcyi.plugin.dunderall.Zoo.message
import cn.aixcyi.plugin.dunderall.utils.getEditor
import cn.aixcyi.plugin.dunderall.utils.getPyFile
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.jetbrains.python.psi.PyFile

abstract class WritableAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getEditor(true) ?: return
        val file = event.getPyFile() ?: return
        val handler = ReadonlyStatusHandler.getInstance(file.project)
        val status = handler.ensureFilesWritable(listOf(file.virtualFile))
        if (status.hasReadonlyFiles()) {
            HintManager.getInstance().showErrorHint(editor, message("hint.EditorIsNotWritable.text"))
            return
        }
        this.actionPerformed(event, editor, file)
    }

    abstract fun actionPerformed(event: AnActionEvent, editor: Editor, file: PyFile)
}