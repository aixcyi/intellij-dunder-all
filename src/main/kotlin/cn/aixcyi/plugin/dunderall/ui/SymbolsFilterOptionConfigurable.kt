package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.I18nProvider.message
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SymbolsFilterOptionConfigurable(private val project: Project) : SearchableConfigurable {

    companion object {
        const val ID = "HooTool.DunderAllFilterOption"
    }

    private var component: SymbolsFilterOptionComponent? = null

    override fun getId(): String = ID

    override fun getDisplayName(): String = message("configurable.HooTool.DunderAllFilterOption.display_name")

    override fun createComponent(): JComponent {
        component = SymbolsFilterOptionComponent(project)
        return component!!.rootPanel
    }

    override fun isModified(): Boolean {
        return component?.isModified() ?: false
    }

    override fun apply() {
        component?.apply()
    }

    override fun reset() {
        component?.reset()
    }

    override fun disposeUIResources() {
        component = null
    }
}