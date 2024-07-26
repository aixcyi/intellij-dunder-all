package cn.aixcyi.plugin.dunderall.services

import cn.aixcyi.plugin.dunderall.Zoo
import com.intellij.openapi.components.*

/**
 * `__all__` 格式化设置。
 *
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
@Service(Service.Level.APP)
@State(name = Zoo.XmlComponent.OPTIMIZATION, storages = [Storage(Zoo.PLUGIN_STORAGE)])
class DunderAllOptimization : SimplePersistentStateComponent<DunderAllOptimization.State>(State()) {

    companion object {
        fun getInstance() = service<DunderAllOptimization>()
    }

    class State : BaseState() {
        var mySequenceOrder by property(Order.APPEARANCE) { it == Order.APPEARANCE }
        var isUseSingleQuote by property(false)
        var isEndsWithComma by property(false)
        var isLineByLine by property(false)
        var isAutoRemoveNonexistence by property(false)
    }

    /** 序列字面值中各个元素的排序方式。 */
    enum class Order {

        /** 按符号出现顺序排序。 */
        APPEARANCE,

        /** 按字母先后顺序排序（不区分大小写）。 */
        ALPHABET,

        /** 按字母先后顺序排序（区分大小写）。 */
        CHARSET;

        override fun toString() = this.ordinal.toString()  // 从0开始
    }
}