package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.I18nProvider.message
import cn.aixcyi.plugin.dunderall.services.DunderAllOptimization
import cn.aixcyi.plugin.dunderall.utils.TopSymbolsHandler
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
import net.aixcyi.utils.AlignX
import net.aixcyi.utils.AlignY
import net.aixcyi.utils.align
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
class DunderAllGenerator(file: PyFile, private val withImports: Boolean) : DialogWrapper(true) {

    private val dunderAll = file.dunderAll ?: listOf()
    private val handler = TopSymbolsHandler(withImports).init(file)
    private val scopes = handler.symbols.values.toMutableSet()
    private val model = CollectionListModel(handler.symbols.toList())
    private val list = JBList(model)
    private var order = DunderAllOptimization.getInstance().state.mySequenceOrder

    init {
        setSize(300, 500)
        setOKButtonText(message("button.OK.text"))
        setCancelButtonText(message("button.Cancel.text"))
        isResizable = true
        title = message("dialog.DunderAllGenerator.title")
        list.setEmptyText(message("dialog.DunderAllGenerator.empty_text"))
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
                .align(AlignX.FILL, AlignY.FILL)
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
        group.addSeparator()
        group.add(
            ScopeToggleAction(message("action.ShowClass.text"), "", AllIcons.Nodes.Class)
                .bind(this)
        )
        group.add(
            ScopeToggleAction(message("action.ShowFunction.text"), "", AllIcons.Nodes.Function)
                .bind(this)
        )
        group.add(
            ScopeToggleAction(message("action.ShowVariable.text"), "", AllIcons.Nodes.Variable)
                .bind(this)
        )
        if (this.withImports)
            group.add(
                ScopeToggleAction(message("action.ShowImports.text"), "", AllIcons.Nodes.Include)
                    .bind(this)
            )
        return group
    }

    private fun updateItems() {
        val pairs = handler.symbols.toList()
            .filter { this.scopes.contains(it.second) }
            .sortedWith(handler.getPairComparator(this.order))
        this.model.removeAll()
        this.model.addAll(0, pairs)
    }

    /**
     * 排序方式切换事件。
     */
    private class SortingToggleAction(
        text: String, description: String, icon: Icon
    ) : ToggleAction(text, description, icon), DumbAware {

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
            if (!state)
                return  // 组合单选，所以不能取消选择
            parent.order = this.target
            parent.updateItems()
        }
    }

    private class ScopeToggleAction(
        text: String, description: String, val icon: Icon
    ) : ToggleAction(text, description, icon), DumbAware {

        private lateinit var parent: DunderAllGenerator

        fun bind(selector: DunderAllGenerator): ScopeToggleAction {
            this.parent = selector
            return this
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT

        override fun isSelected(e: AnActionEvent): Boolean {
            return parent.scopes.contains(this.icon)
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state)
                parent.scopes.add(this.icon)
            else
                parent.scopes.remove(this.icon)

            parent.updateItems()
        }
    }
}