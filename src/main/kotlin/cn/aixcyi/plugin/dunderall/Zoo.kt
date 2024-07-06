package cn.aixcyi.plugin.dunderall

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object Zoo {
    /**
     * 插件配置的全局存储位置。
     */
    const val PLUGIN_STORAGE = "HooTool.DunderAll.xml"

    /**
     * 配置所对应的 XML 的组件名。
     *
     * `<component name="NAME"></component>`
     */
    object XmlComponent {
        const val OPTIMIZATION = "HooTool.DunderAll.Optimization"
    }

    /** 国际化文本资源包。 */
    private val BUNDLE = ResourceBundle.getBundle("messages.PythonDunderAll", DynamicBundle.getLocale())

    /**
     * 获取本地化翻译。
     *
     * Retrieve translated text via keys on "properties" file.
     *
     * @param key properties 文件中的键。
     * @return properties 文件中键对应的值。
     */
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = "messages.PythonDunderAll") key: String): String = BUNDLE.getString(key)
}