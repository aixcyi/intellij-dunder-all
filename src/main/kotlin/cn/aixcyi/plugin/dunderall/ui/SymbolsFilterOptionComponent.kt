package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.I18nProvider.message
import cn.aixcyi.plugin.dunderall.services.SymbolsFilterOptions
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.bindSelected
import com.jetbrains.python.PyNames


class SymbolsFilterOptionComponent(project: Project) {

    private val options = SymbolsFilterOptions.getInstance(project).state

    private val tabClass = SymbolsFilterOptionTab(
        message("tab.ClassFilterOptions.title"),
        AllIcons.Nodes.Class,
        options::classFilterLevel,
        options.blacklistClassName,
    )

    private val tabFunction = SymbolsFilterOptionTab(
        message("tab.FunctionFilterOptions.title"),
        AllIcons.Nodes.Function,
        options::functionFilterLevel,
        options.blacklistFunctionName,
    )

    private val tabImports = SymbolsFilterOptionTab(
        message("tab.ImportsFilterOptions.title"),
        AllIcons.Nodes.Include,
        options::importsFilterLevel,
        options.blacklistImportsName,
    )

    private val tabVariable = SymbolsFilterOptionTab(
        message("tab.VariableFilterOptions.title"),
        AllIcons.Nodes.Variable,
        options::variableFilterLevel,
        options.blacklistVariableName,
    ) {
        checkBox(message("checkbox.ExcludeDunderAttribute.text"))
            .comment(message("label.WhyCannotContainDunderAll.text"))
            .bindSelected(options::isExcludeDunderAttribute)
        contextHelp(
            PyNames.UNDERSCORED_ATTRIBUTES
                .sorted()
                .chunked(4)
                .joinToString("", "<table>", "</table>") { tuple ->
                    tuple.joinToString("", "<tr>", "</tr>") { "<td>$it</td>" }
                }
        )
    }

    private val tabs = listOf(
        tabClass,
        tabFunction,
        tabVariable,
        tabImports,
    )

    val rootPanel = JBTabbedPane().apply {
        for (tab in tabs) {
            addTab(tab.title, tab.icon, tab.rootPanel)
        }
    }

    fun isModified() = tabs.any(SymbolsFilterOptionTab::isModified)

    fun apply() {
        tabs.forEach(SymbolsFilterOptionTab::apply)
    }

    fun reset() {
        tabs.forEach(SymbolsFilterOptionTab::reset)
    }
}