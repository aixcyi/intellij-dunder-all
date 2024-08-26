package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.I18nProvider.message
import cn.aixcyi.plugin.dunderall.services.SymbolsFilterOptions.Level
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindValue
import com.intellij.ui.dsl.builder.panel
import net.aixcyi.shim.AlignX
import net.aixcyi.shim.AlignY
import net.aixcyi.shim.align
import java.util.*
import javax.swing.Icon
import javax.swing.JLabel
import kotlin.reflect.KMutableProperty0

class SymbolsFilterOptionTab(
    val title: String,
    val icon: Icon,
    private val filterLevel: KMutableProperty0<Level>,
    private val blacklist: MutableSet<String>,
    private val additions: (Row.() -> Unit)? = null,
) {
    private val model = CollectionListModel(blacklist)
    private val list = JBList(model).apply { setEmptyText(message("list.Generic.empty_text")) }
    private val levelTextMap = Hashtable<Int, JLabel>(3, 1.0F).apply {
        put(Level.ONLY_PUBLIC.ordinal, JLabel(message("label.FilterLevel.OnlyPublic.text")))
        put(Level.NON_PRIVATE.ordinal, JLabel(message("label.FilterLevel.NonPrivate.text")))
        put(Level.ALL.ordinal, JLabel(message("label.FilterLevel.All.text")))
    }

    private var levelInteger: Int = filterLevel.get().ordinal

    private val listPanel = ToolbarDecorator.createDecorator(list)
        .setAddAction {
            val input = Messages.showInputDialog("", message("dialog.SymbolsBlacklistInput.title"), null)
            if (input.isNullOrBlank()) {
                return@setAddAction
            }
            if (input in blacklist) {
                list.selectedIndex = model.getElementIndex(input)
            } else {
                model.add(input)
                list.selectedIndex = list.itemsCount - 1
            }
        }
        .setEditAction {
            val input = Messages.showInputDialog(
                "",
                message("dialog.SymbolsBlacklistInput.title"),
                null,
                model.getElementAt(list.selectedIndex),
                null,
            )
            if (input.isNullOrBlank() || input in blacklist)
                return@setEditAction
            model.setElementAt(input, list.selectedIndex)
        }
        .setRemoveAction {
            model.remove(list.selectedIndex)
            list.selectionModel.leadSelectionIndex = list.leadSelectionIndex
        }
        .createPanel()

    val rootPanel = panel {
        row(message("label.FilterLevel.text")) {
            slider(0, Level.values().lastIndex, 1, 1)
                .bindValue(::levelInteger)
                .applyToComponent {
                    labelTable = levelTextMap
                }
                .onIsModified {
                    levelInteger != filterLevel.get().ordinal
                }
                .onReset {
                    levelInteger = filterLevel.get().ordinal
                }
                .onApply {
                    filterLevel.set(Level.values()[levelInteger])
                }
            contextHelp(
                message("label.FilterLevelDescription.text"),
            )
        }
        additions?.let {
            row(message("label.MoreOptions.text")) {
                additions.invoke(this)
            }
        }
        row {
            resizableRow()
            cell(listPanel)
                .label(message("label.SymbolsBlacklist.text"), LabelPosition.TOP)
                .align(AlignX.FILL, AlignY.FILL)
                .onIsModified {
                    model.toList().toSet() != blacklist
                }
                .onReset {
                    model.replaceAll(blacklist.toList())
                }
                .onApply {
                    blacklist.clear()
                    blacklist.addAll(model.toList())
                }
        }
    }

    fun isModified(): Boolean = rootPanel.isModified()

    fun apply() = rootPanel.apply()

    fun reset() = rootPanel.reset()
}