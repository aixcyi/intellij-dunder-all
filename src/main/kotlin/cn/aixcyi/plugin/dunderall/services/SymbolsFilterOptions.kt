package cn.aixcyi.plugin.dunderall.services

import cn.aixcyi.plugin.dunderall.Zoo
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project


/**
 * 符号过滤选项（项目级）。
 *
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
@Service(Service.Level.PROJECT)
@State(name = Zoo.XmlComponent.SYMBOLS_FILTER_OPTIONS, storages = [Storage(Zoo.PLUGIN_STORAGE)])
class SymbolsFilterOptions : SimplePersistentStateComponent<SymbolsFilterOptions.State>(State()) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): SymbolsFilterOptions = project.service()
    }

    class State : BaseState() {
        var classFilterLevel by property(Level.ONLY_PUBLIC) { it == Level.ONLY_PUBLIC }
        var blacklistClassName by list<String>()

        var functionFilterLevel by property(Level.ONLY_PUBLIC) { it == Level.ONLY_PUBLIC }
        var blacklistFunctionName by list<String>()

        var variableFilterLevel by property(Level.ONLY_PUBLIC) { it == Level.ONLY_PUBLIC }
        var isExcludeDunderAttribute by property(false)
        var blacklistVariableName by list<String>()

        var importsFilterLevel by property(Level.ONLY_PUBLIC) { it == Level.ONLY_PUBLIC }
        var blacklistImportsName by list<String>()
    }

    enum class Level {
        ONLY_PUBLIC,
        NON_PRIVATE,
        ALL;
    }
}