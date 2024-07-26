package cn.aixcyi.plugin.dunderall.utils

import cn.aixcyi.plugin.dunderall.services.DunderAllOptimization
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyElementGeneratorImpl
import com.jetbrains.python.psi.impl.PyExpressionStatementImpl

/**
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
class DunderAllWrapper(private val file: PyFile) {

    companion object {
        /**
         * 构造仅包含 `string` 的带格式的 `list` 字面值。
         *
         * @param strings `list` 中的各个字符串。不必也不能预先添加引号。
         * @param state 格式化参数。
         * @return 字面值所对应的字符串。
         */
        fun buildStringList(
            strings: List<String>,
            state: DunderAllOptimization.State = DunderAllOptimization.getInstance().state
        ): String {
            val quote = if (state.isUseSingleQuote) "'" else "\""
            return if (state.isEndsWithComma)
                strings.joinToString(
                    separator = if (state.isLineByLine) "\n" else " ",
                    postfix = if (state.isLineByLine) "\n]" else "]",
                    prefix = if (state.isLineByLine) "[\n" else "[",
                ) { "$quote$it$quote," }
            else
                strings.joinToString(
                    separator = if (state.isLineByLine) ",\n" else ", ",
                    postfix = if (state.isLineByLine) "\n]" else "]",
                    prefix = if (state.isLineByLine) "[\n" else "[",
                ) { "$quote$it$quote" }
        }
    }

    /** 通过 `__all__` 导出的所有符号。 */
    val exports = file.dunderAll ?: listOf()

    /** `__all__` 赋值表达式。 */
    val expression = file.findTopLevelAttribute(PyNames.ALL)

    /** `__all__` 赋值表达式中的值的部分。 */
    val assignment = expression?.findAssignedValue()

    /** 对当前 `__all__` 重新赋值。注意：[assignment] 必须为非空值。 */
    fun reassign(symbols: List<String>) {
        val code = buildStringList(symbols)
        val proxy = PyElementGeneratorImpl(file.project)
        val snippet = proxy.createFromText(file.languageLevel, PyExpressionStatementImpl::class.java, code)
        assignment!!.replace(snippet)
    }

    /** 构造 `__all__` 的赋值表达式。 */
    fun express(symbols: List<String>): PyAssignmentStatement {
        val code = "${PyNames.ALL} = ${buildStringList(symbols)}"
        val proxy = PyElementGeneratorImpl(file.project)
        return proxy.createFromText(file.languageLevel, PyAssignmentStatement::class.java, code)
    }

    /**
     * `__all__` 的值是否合法。
     *
     * @see <a href="https://docs.python.org/3/reference/simple_stmts.html#the-import-statement">The <code>import</code> statement</a>
     */
    fun isValidAssignment() = when (assignment) {
        // __all__ 的值必须是一个字符串序列
        // 也就是说列表、元组、集合都可以视为合法值。
        is PyListLiteralExpression,
        is PySetLiteralExpression,
        is PyTupleExpression -> true
        // 注意这里判断的是字面值，所以没有其它 sequence 类型。
        else -> false
    }
}