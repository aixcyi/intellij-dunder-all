package cn.aixcyi.plugin.dunderall.utils

import com.intellij.openapi.util.text.TextWithMnemonic
import com.intellij.ui.dsl.builder.Cell
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * 自动解析 Kotlin UI DSL [Cell] 包装的组件的文本，并设置助记键。
 */
fun <T : JComponent> Cell<T>.mnemonic(): Cell<T> {
    when (val component = this.component) {
        // 按钮、单选框、复选框
        is AbstractButton -> {
            val text = TextWithMnemonic.parse(component.text)
            if (text.hasMnemonic()) {
                component.mnemonic = text.mnemonicCode
                component.text = text.text
            }
        }
        // 标签
        is JLabel -> {
            val text = TextWithMnemonic.parse(component.text)
            if (text.hasMnemonic()) {
                component.setDisplayedMnemonic(text.mnemonicChar)
                component.text = text.text
            }
        }
        // 其它组件
        else -> {}
    }
    return this
}