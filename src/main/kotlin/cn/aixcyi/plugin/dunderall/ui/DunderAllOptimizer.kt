package cn.aixcyi.plugin.dunderall.ui

import cn.aixcyi.plugin.dunderall.I18nProvider.message
import cn.aixcyi.plugin.dunderall.services.DunderAllOptimization
import cn.aixcyi.plugin.dunderall.services.DunderAllOptimization.Order
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import net.aixcyi.utils.mnemonic


/**
 * `__all__` 格式化设置面板。
 *
 * @author <a href="https://github.com/aixcyi">砹小翼</a>
 */
class DunderAllOptimizer : DialogWrapper(true) {

    val state = DunderAllOptimization.getInstance().state

    init {
        isResizable = false
        title = message("command.OptimizeDunderAll")
        setOKButtonText(message("button.OK.text"))
        setCancelButtonText(message("button.Cancel.text"))
        super.init()
    }

    override fun createCenterPanel() = panel {
        group(message("label.DunderAllOptimizerOrdering.text")) {
            buttonsGroup {
                row {
                    radioButton(message("radio.DunderAllOrderByAppearance.text"), Order.APPEARANCE)
                        .mnemonic()
                }
                row {
                    radioButton(message("radio.DunderAllOrderByAlphabet.text"), Order.ALPHABET)
                        .mnemonic()
                }
                row {
                    radioButton(message("radio.DunderAllOrderByCharacter.text"), Order.CHARSET)
                        .mnemonic()
                }
            }.bind(state::mySequenceOrder)
        }
        group(message("label.DunderAllOptimizerOptions.text")) {
            row {
                checkBox(message("checkbox.DunderAllEndsWithComma.text"))
                    .bindSelected(state::isEndsWithComma)
                    .mnemonic()
            }
            row {
                checkBox(message("checkbox.DunderAllUseSingleQuote.text"))
                    .bindSelected(state::isUseSingleQuote)
                    .mnemonic()
            }
            row {
                checkBox(message("checkbox.DunderAllLineByLine.text"))
                    .bindSelected(state::isLineByLine)
                    .mnemonic()
            }
            row {
                checkBox(message("checkbox.DunderAllRemoveNonexistence.text"))
                    .bindSelected(state::isAutoRemoveNonexistence)
                    .mnemonic()
            }
        }
    }
}