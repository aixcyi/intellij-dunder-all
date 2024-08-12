package cn.aixcyi.plugin.dunderall.utils

import cn.aixcyi.plugin.dunderall.AppIcons
import cn.aixcyi.plugin.dunderall.services.DunderAllOptimization.Order
import cn.aixcyi.plugin.dunderall.services.SymbolsFilterOptions
import com.intellij.icons.AllIcons
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.*
import javax.swing.Icon

/**
 * 文件内的所有顶层符号。
 *
 * 类内部手动枚举所有符号，并递归筛选顶层符号。手动枚举是为了让不同类型的符号混合之后保持定义顺序。
 *
 * @param withImports 是否包括导入。因为存在 `from xxx import *` 这样的语句，所以搜索导入的话会消耗更多时间和内存。
 * @param depth 解析导入语句时的递归深度。深度低于 `1` 则终止。
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
class TopSymbolsHandler(private val withImports: Boolean = false, private val depth: Int = 5) {

    private val visitedFiles = mutableSetOf<PyFile>()
    private lateinit var options: SymbolsFilterOptions.State

    /** 文件顶级作用域内定义的所有符号。是否包含导入的符号取决于 [withImports] 。 */
    val symbols = mutableMapOf<String, Icon>()

    /** 递归枚举 [file] 内的符号，并初始化 [TopSymbolsHandler.symbols] 。 */
    fun init(file: PyFile): TopSymbolsHandler {
        symbols.clear()
        visitedFiles.clear()
        options = SymbolsFilterOptions.getInstance(file.project).state
        file.statements.forEach(::collect)
        return this
    }

    /**
     * 获取不同 [order] 时符号的比较器。
     */
    fun getSymbolComparator(order: Order): Comparator<String> {
        val names = symbols.keys.toList()
        return when (order) {
            Order.CHARSET -> Comparator { a, b -> a.compareTo(b) }
            Order.ALPHABET -> Comparator { a, b -> a.compareTo(b, ignoreCase = true) }
            Order.APPEARANCE -> Comparator { a, b -> names.indexOf(a).compareTo(names.indexOf(b)) }
        }
    }

    /**
     * 获取不同 [order] 时符号-图标对的比较器。
     */
    fun getPairComparator(order: Order): Comparator<Pair<String, Icon>> {
        val names = symbols.keys.toList()
        return when (order) {
            Order.CHARSET -> Comparator { a, b -> a.first.compareTo(b.first) }
            Order.ALPHABET -> Comparator { a, b -> a.first.compareTo(b.first, ignoreCase = true) }
            Order.APPEARANCE -> Comparator { a, b -> names.indexOf(a.first).compareTo(names.indexOf(b.first)) }
        }
    }

    /**
     * 移除 [list] 中不存在于文件内的符号。
     */
    fun remove(list: MutableList<String>) {
        for (index in list.indices.reversed()) {
            if (!symbols.keys.contains(list[index])) {
                list.removeAt(index)
            }
        }
    }

    /**
     * 解析并收集表达式 [statement] 的符号，包括
     *
     * - 类定义中的类名
     * - 函数定义中的函数名
     * - 赋值语句、判断语句中定义的变量名
     * - 导入语句所导入的符号名或其别名
     */
    private fun collect(statement: PyStatement) {
        // 类定义
        if (statement is PyClass) {
            val className = SymbolName(statement.name ?: return)
            if (className.level > options.classFilterLevel) return
            if (className.name in options.blacklistClassName) return
            symbols[className.name] =
                if (className.isNonPublic) AppIcons.NonPublicClass else AllIcons.Nodes.Class
        }
        // 函数定义
        else if (statement is PyFunction) {
            val funcName = SymbolName(statement.name ?: return)
            if (funcName.level > options.functionFilterLevel) return
            if (funcName.name in options.blacklistFunctionName) return
            symbols[funcName.name] =
                if (funcName.isNonPublic) AppIcons.NonPublicFunction else AllIcons.Nodes.Function
        }
        // 赋值表达式
        else if (statement is PyAssignmentStatement) {
            // 因为赋值表达式存在元组解包的情况，
            // 所以需要用循环来提取普通变量
            for (pair in statement.targetsToValuesMapping) {
                val variable = pair.first
                val variableName = SymbolName(pair.first.name ?: continue)
                // 过滤掉属性，比如 meow.age = 6
                if (variable is PyTargetExpression) {
                    val qName = variable.asQualifiedName()
                    if (qName?.toString() != variableName.name) {
                        continue
                    }
                }
                if (variableName.name == PyNames.ALL) continue
                if (variableName.isDunderAttribute && options.isExcludeDunderAttribute) continue
                if (variableName.level > options.variableFilterLevel) continue
                if (variableName.name in options.blacklistVariableName) continue
                symbols[variableName.name] =
                    if (variableName.isDunderAttribute)
                        AppIcons.DunderVariable
                    else if (variableName.isConstant)
                        if (variableName.isNonPublic) AppIcons.NonPublicConstant else AllIcons.Nodes.Constant
                    else
                        if (variableName.isNonPublic) AppIcons.NonPublicVariable else AllIcons.Nodes.Variable
            }
        }
        // 判断语句
        else if (statement is PyIfStatement) {
            for (ps in statement.getIfPart().statementList.statements) {
                this.collect(ps)
            }
        }
        // from 根包.子包 import 孙包
        // from 根包.子包 import 孙包 as 别名
        // from 根包.子包.孙包 import 符号
        // from 根包.子包.孙包 import 符号 as 别名
        // from 根包.子包.孙包 import *
        else if (withImports && statement is PyFromImportStatement) {
            if (statement.isFromFuture)
                return

            val imports =
                if (statement.isStarImport)
                    this.parseStarImport(statement)
                else
                    statement.importElements.mapNotNull { it.visibleName }

            symbols.putAll(
                imports
                    .map(::SymbolName)
                    .filterNot { it.level > options.importsFilterLevel || it.name in options.blacklistImportsName }
                    .map {
                        it.name to if (it.isNonPublic)
                            AppIcons.NonPublicImported
                        else
                            AllIcons.Nodes.Include
                    }
            )
        }
        // import 根包
        // import 根包.子包
        // import 根包.子包.孙包
        // import 根包.子包.孙包 as 孙包别名
        else if (withImports && statement is PyImportStatement) {
            symbols.putAll(
                statement.importElements
                    .mapNotNull { it.visibleName?.let { name -> SymbolName(name) } }
                    .filterNot { it.level > options.importsFilterLevel || it.name in options.blacklistImportsName }
                    .map {
                        it.name to if (it.isNonPublic)
                            AppIcons.NonPublicImported
                        else
                            AllIcons.Nodes.Include
                    }
            )
        }
    }

    /**
     * 解析 `from xxx import *` 所导入的符号。
     *
     * - 如果包提供了 `__all__` 的话会直接合入这个列表。
     * - 如果包没有提供 `__all__` 则使用 [TopSymbolsHandler] 递归搜索顶级符号。
     */
    private fun parseStarImport(statement: PyFromImportStatement): MutableList<String> {
        if (depth <= 0)
            return mutableListOf()
        val file = PyUtil.turnDirIntoInit(statement.resolveImportSource())
        if (file !is PyFile || file in visitedFiles)
            return mutableListOf()
        visitedFiles.add(file)

        return file.dunderAll ?: run {
            // 定义的符号有专属图标，而导入的不一定能识别出来，所以统一为导入的图标。
            TopSymbolsHandler(withImports, depth - 1)
                .init(file)
                .symbols
                .keys
                .toMutableList()
        }
    }

    data class SymbolName(val name: String) {
        val level: SymbolsFilterOptions.Level =
            if (name.startsWith("__") && name.endsWith("__"))
                SymbolsFilterOptions.Level.ONLY_PUBLIC
            else if (name.startsWith("__"))
                SymbolsFilterOptions.Level.ALL
            else if (name.startsWith("_"))
                SymbolsFilterOptions.Level.NON_PRIVATE
            else
                SymbolsFilterOptions.Level.ONLY_PUBLIC

        val isNonPublic = level > SymbolsFilterOptions.Level.ONLY_PUBLIC

        val isConstant: Boolean by lazy { name == name.uppercase() }

        val isDunderAttribute: Boolean by lazy { name in PyNames.UNDERSCORED_ATTRIBUTES }
    }
}