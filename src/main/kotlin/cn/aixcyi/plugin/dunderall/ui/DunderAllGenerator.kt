package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.AppIcons
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
import kotlinx.collections.immutable.toImmutableSet
import net.aixcyi.shim.AlignX
import net.aixcyi.shim.AlignY
import net.aixcyi.shim.align
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
    private val model = CollectionListModel(handler.symbols.toList())
    private val list = JBList(model)
    private val rawScopes = handler.symbols.values.toImmutableSet()
    private val currScopes = rawScopes.toMutableSet()

    private var order = DunderAllOptimization.getInstance().state.mySequenceOrder

    init {
        setSize(300, 500)
        setOKButtonText(message("button.OK.text"))
        setCancelButtonText(message("button.Cancel.text"))
        isResizable = true
        title = message("dialog.DunderAllGenerator.title")
        list.setEmptyText(message("list.Generic.empty_text"))
        list.setCellRenderer(object : ColoredListCellRenderer<Pair<String, Icon>>() {
            override fun customizeCellRenderer(
                list: JList<out Pair<String, Icon>>,
                value: Pair<String, Icon>,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
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
        // TODO: 将排序从【切换改】为【下拉单选】
        group.addAll(
            SortingToggleAction(
                message("action.SortByAppearance.text"),
                AllIcons.ObjectBrowser.VisibilitySort,
                DunderAllOptimization.Order.APPEARANCE,
                this,
            ),
            SortingToggleAction(
                message("action.SortByAlphabet.text"),
                AllIcons.ObjectBrowser.Sorted,
                DunderAllOptimization.Order.ALPHABET,
                this,
            ),
            SortingToggleAction(
                message("action.SortByCharset.text"),
                AllIcons.ObjectBrowser.SortByType,
                DunderAllOptimization.Order.CHARSET,
                this,
            ),
        )
        group.addSeparator()
        group.addAll(
            ScopeToggleAction(message("action.ShowClasses.text"), this, AllIcons.Nodes.Class, AppIcons.NonPublicClass),
            ScopeToggleAction(
                message("action.ShowFunctions.text"),
                this,
                AllIcons.Nodes.Function,
                AppIcons.NonPublicFunction
            ),
            ScopeToggleAction(
                message("action.ShowVariables.text"),
                this,
                AllIcons.Nodes.Variable,
                AppIcons.NonPublicVariable
            ),
            ScopeToggleAction(
                message("action.ShowConstants.text"),
                this,
                AllIcons.Nodes.Constant,
                AppIcons.NonPublicConstant
            ),
            ScopeToggleAction(message("action.ShowDunderAttributes.text"), this, AppIcons.DunderVariable),
        )
        if (this.withImports)
            group.add(
                ScopeToggleAction(message("action.ShowImports.text"), this, AllIcons.Nodes.Include)
            )
        return group
    }

    private fun updateItems() {
        this.model.replaceAll(
            handler.symbols.toList()
                .filter { this.currScopes.contains(it.second) }
                .sortedWith(handler.getPairComparator(this.order))
        )
    }

    /**
     * 排序方式切换事件。
     */
    private class SortingToggleAction(
        text: String,
        icon: Icon,
        val target: DunderAllOptimization.Order,
        val parent: DunderAllGenerator,
    ) : ToggleAction(text, null, icon), DumbAware {

        override fun getActionUpdateThread() = ActionUpdateThread.EDT

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

    /**
     * 范围选择事件。[icons] 必须提供至少一个元素，用作 [ToggleAction] 的图标。
     */
    private class ScopeToggleAction(
        text: String,
        val parent: DunderAllGenerator,
        vararg icons: Icon,
    ) : ToggleAction(text, null, icons[0]), DumbAware {

        private val iconSet = icons.toSet()
        private val isVisible = iconSet.any { it in parent.rawScopes }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isVisible = isVisible
            super.update(e)
        }

        override fun isSelected(e: AnActionEvent): Boolean {
            return iconSet.any { it in parent.currScopes }
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state)
                parent.currScopes.addAll(iconSet)
            else
                parent.currScopes.removeAll(iconSet)

            parent.updateItems()
        }
    }
}