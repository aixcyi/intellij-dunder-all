package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.AppIcons
import cn.aixcyi.plugin.dunderall.I18nProvider.message
import cn.aixcyi.plugin.dunderall.services.DunderAllOptimization
import cn.aixcyi.plugin.dunderall.utils.TopSymbolsHandler
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
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
        // TODO: 将排序从【切换】改为【下拉单选】
        // @formatter:off
        val actions = mutableListOf(
            SortingToggleAction(DunderAllOptimization.Order.APPEARANCE, message("action.SortByAppearance.text"), AllIcons.ObjectBrowser.VisibilitySort),
            SortingToggleAction(DunderAllOptimization.Order.ALPHABET, message("action.SortByAlphabet.text"), AllIcons.ObjectBrowser.Sorted),
            SortingToggleAction(DunderAllOptimization.Order.CHARSET, message("action.SortByCharset.text"), AllIcons.ObjectBrowser.SortByType),
            Separator.create(),
            ScopeToggleAction(message("action.ShowClasses.text"), AllIcons.Nodes.Class, AppIcons.NonPublicClass),
            ScopeToggleAction(message("action.ShowFunctions.text"), AllIcons.Nodes.Function, AppIcons.NonPublicFunction),
            ScopeToggleAction(message("action.ShowVariables.text"), AllIcons.Nodes.Variable, AppIcons.NonPublicVariable),
            ScopeToggleAction(message("action.ShowConstants.text"), AllIcons.Nodes.Constant, AppIcons.NonPublicConstant),
            ScopeToggleAction(message("action.ShowDunderAttributes.text"), AppIcons.DunderVariable),
        )
        // @formatter:on
        if (this.withImports)
            actions.add(
                ScopeToggleAction(message("action.ShowImports.text"), AllIcons.Nodes.Include),
            )
        actions.forEach {
            when (it) {
                is ScopeToggleAction -> it.parent = this
                is SortingToggleAction -> it.parent = this
            }
        }
        return DefaultActionGroup().apply { addAll(actions) }
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
        val target: DunderAllOptimization.Order,
        text: String,
        icon: Icon,
    ) : ToggleAction(text, null, icon), DumbAware {

        lateinit var parent: DunderAllGenerator

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
        vararg icons: Icon,
    ) : ToggleAction(text, null, icons[0]), DumbAware {

        private val iconSet = icons.toSet()
        private val isVisible by lazy { iconSet.any { it in parent.rawScopes } }

        lateinit var parent: DunderAllGenerator

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