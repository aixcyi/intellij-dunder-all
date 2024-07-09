package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.Zoo.message
import cn.aixcyi.plugin.dunderall.storage.DunderAllOptimization
import cn.aixcyi.plugin.dunderall.utils.TopSymbolsHandler
import cn.aixcyi.plugin.dunderall.utils.hFill
import cn.aixcyi.plugin.dunderall.utils.vFill
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.python.psi.PyFile
import javax.swing.Icon
import javax.swing.JList

/**
 * `__all__` 生成对话框。
 *
 * - 会覆盖已有的 `__all__` 。
 * - 已导出的符号会显示为灰色。
 * - 所有符号均包含图标，而导入的符号统一为导入的图标，不再细分。
 *
 * @param file 已打开的 Python 文件。
 * @param withImports 是否包含导入的符号。是则递归提取。
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
class DunderAllGenerator(file: PyFile, withImports: Boolean) : DialogWrapper(true) {

    private val dunderAll = file.dunderAll ?: listOf()
    private val handler = TopSymbolsHandler(file, withImports)
    private val model = CollectionListModel(handler.symbols.toList())
    private val list = JBList(model)
    private var order = DunderAllOptimization.getInstance().state.mySequenceOrder

    init {
        setSize(300, 500)
        setOKButtonText(message("button.OK.text"))
        setCancelButtonText(message("button.Cancel.text"))
        isResizable = true
        title = message("dialog.DunderAllGenerator.title")
        list.emptyText.appendLine(message("dialog.DunderAllGenerator.empty_text.1"))
        list.emptyText.appendLine(message("dialog.DunderAllGenerator.empty_text.2"))
        list.setCellRenderer(object : ColoredListCellRenderer<Pair<String, Icon>>() {
            override fun customizeCellRenderer(
                list: JList<out Pair<String, Icon>>,
                value: Pair<String, Icon>,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                this.append(value.first)
                this.icon = value.second
                this.isEnabled = !dunderAll.contains(value.first)
            }
        })
        super.init()
    }

    /** 确认后返回所有选中的符号，否则返回 `null` 。 */
    fun showThenGet(): List<String>? {
        if (!showAndGet())
            return null
        return list.selectedValuesList.map { it.first }.toList()
    }

    override fun createCenterPanel() = panel {
        row {
            resizableRow()
            val listPanel = ToolbarDecorator.createDecorator(list)
                .disableAddAction()
                .disableRemoveAction()
                .disableUpAction()
                .disableDownAction()
                .disableUpDownActions()
                .setActionGroup(prepareToolbarActions())
                .createPanel()
            cell(listPanel)
                .hFill()
                .vFill()
        }
    }

    override fun getPreferredFocusedComponent() = list

    override fun doValidate(): ValidationInfo? {
        if (list.selectionModel.selectedItemsCount > 0)
            return null
        return ValidationInfo(message("validation.NoSymbolsChosen"), list)
    }

    private fun prepareToolbarActions(): DefaultActionGroup {
        val group = DefaultActionGroup()
        group.add(
            SortingToggleAction(message("action.SortByAppearance.text"), "", AllIcons.ObjectBrowser.VisibilitySort)
                .bind(this)
                .anchor(DunderAllOptimization.Order.APPEARANCE)
        )
        group.add(
            SortingToggleAction(message("action.SortByAlphabet.text"), "", AllIcons.ObjectBrowser.Sorted)
                .bind(this)
                .anchor(DunderAllOptimization.Order.ALPHABET)
        )
        group.add(
            SortingToggleAction(message("action.SortByCharset.text"), "", AllIcons.ObjectBrowser.SortByType)
                .bind(this)
                .anchor(DunderAllOptimization.Order.CHARSET)
        )
        return group
    }

    /**
     * 排序方式切换事件。
     */
    private class SortingToggleAction(
        title: String, description: String, icon: Icon
    ) : ToggleAction(title, description, icon), DumbAware {

        private lateinit var parent: DunderAllGenerator
        private lateinit var target: DunderAllOptimization.Order

        fun bind(selector: DunderAllGenerator): SortingToggleAction {
            this.parent = selector
            return this
        }

        fun anchor(order: DunderAllOptimization.Order): SortingToggleAction {
            this.target = order
            return this
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT

        override fun isSelected(e: AnActionEvent): Boolean {
            return parent.order == this.target
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (!state) return
            val handler = parent.handler
            val pairs = handler.symbols.toList().sortedWith(handler.getPairComparator(this.target))
            parent.order = this.target
            parent.model.removeAll()
            parent.model.addAll(0, pairs)
        }
    }
}