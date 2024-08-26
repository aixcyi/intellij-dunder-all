package cn.aixcyi.plugin.dunderall


object Zoo {
    /**
     * 插件配置的全局存储位置。
     */
    const val PLUGIN_STORAGE = "HooTool.xml"

    /**
     * 国际化文本包路径。
     *
     * 对应 `./src/kotlin/resources/messages/PythonDunderAll.properties`
     */
    const val BUNDLE_NAME = "messages.PythonDunderAll"

    /**
     * 配置所对应的 XML 的组件名。
     *
     * `<component name="NAME"></component>`
     */
    object XmlComponent {
        const val OPTIMIZATION = "DunderAll.Optimization"
        const val SYMBOLS_FILTER_OPTIONS = "DunderAll.SymbolsFilterOptions"
    }
}