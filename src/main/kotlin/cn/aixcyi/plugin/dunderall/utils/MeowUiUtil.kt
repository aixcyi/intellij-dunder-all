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

/**
 * 让 [Cell] 横向填满当前单元格。用于兼容以下两种写法：
 *
 * - `align(com.intellij.ui.dsl.builder.AlignX.FILL)`
 * - `horizontalAlign(com.intellij.ui.dsl.gridLayout.HorizontalAlign.FILL)`
 */
fun <T : JComponent> Cell<T>.xFill(): Cell<T> {
    exec {
        val klass = Class.forName("com.intellij.ui.dsl.builder.Align")
        val param = Class.forName("com.intellij.ui.dsl.builder.AlignX")
            .kotlin.sealedSubclasses.first { it.simpleName == "FILL" }
            .objectInstance
        javaClass.getMethod("align", klass).invoke(this, param)
    }?.exec {
        val klass = Class.forName("com.intellij.ui.dsl.gridLayout.HorizontalAlign")
        val param = klass.enumConstants.map { it as Enum<*> }.first { it.name == "FILL" }
        javaClass.getMethod("horizontalAlign", klass).invoke(this, param)
    }
    return this
}

/**
 * 让 [Cell] 纵向填满当前单元格。用于兼容以下两种写法：
 *
 * - `align(com.intellij.ui.dsl.builder.AlignY.FILL)`
 * - `verticalAlign(com.intellij.ui.dsl.gridLayout.VerticalAlign.FILL)`
 */
fun <T : JComponent> Cell<T>.yFill(): Cell<T> {
    exec {
        val klass = Class.forName("com.intellij.ui.dsl.builder.Align")
        val param = Class.forName("com.intellij.ui.dsl.builder.AlignY")
            .kotlin.sealedSubclasses.first { it.simpleName == "FILL" }
            .objectInstance
        javaClass.getMethod("align", klass).invoke(this, param)
    }?.exec {
        val klass = Class.forName("com.intellij.ui.dsl.gridLayout.VerticalAlign")
        val param = klass.enumConstants.map { it as Enum<*> }.first { it.name == "FILL" }
        javaClass.getMethod("verticalAlign", klass).invoke(this, param)
    }
    return this
}