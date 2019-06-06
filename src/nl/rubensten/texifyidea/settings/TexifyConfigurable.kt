package nl.rubensten.texifyidea.settings

import com.intellij.openapi.options.SearchableConfigurable
import nl.rubensten.texifyidea.settings.labeldefiningcommands.TexifyConfigurableLabelCommands
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import nl.rubensten.texifyidea.run.LatexCompiler
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * @author Ruben Schellekens, Sten Wessel
 */
class TexifyConfigurable(private val settings: TexifySettings) : SearchableConfigurable {

    private lateinit var automaticSoftWraps: JBCheckBox
    private lateinit var automaticSecondInlineMathSymbol: JBCheckBox
    private lateinit var automaticUpDownBracket: JBCheckBox
    private lateinit var automaticItemInItemize: JBCheckBox
    private lateinit var automaticQuoteReplacement: ComboBox<String>
    private lateinit var compilerCompatibility: ComboBox<String>
    private lateinit var labelDefiningCommands: TexifyConfigurableLabelCommands

    override fun getId() = "TexifyConfigurable"

    override fun getDisplayName() = "TeXiFy"

    override fun createComponent(): JComponent? {
        labelDefiningCommands = TexifyConfigurableLabelCommands(settings)

        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

            automaticSoftWraps = addCheckbox("Enable soft wraps when opening LaTeX files")
            automaticSecondInlineMathSymbol = addCheckbox("Automatically insert second '$'")
            automaticUpDownBracket = addCheckbox("Automatically insert braces around text in subscript and superscript")
            automaticItemInItemize = addCheckbox("Automatically insert '\\item' in itemize-like environments on pressing enter")
            automaticQuoteReplacement = addSmartQuotesOptions("Off", "TeX ligatures", "TeX commands")
            compilerCompatibility = addCompilerCompatibility()

                add(labelDefiningCommands.getTable())
            })
        }
    }

    /**
     * Add the options for the smart quote substitution.
     */
    private fun JPanel.addSmartQuotesOptions(vararg values: String): ComboBox<String> {
        val list = ComboBox(values)
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply{
            add(JBLabel("Smart quote substitution: "))
            add(list)
        })
        return list
    }

    /**
     * Add the options for the compiler compatibility.
     */
    private fun JPanel.addCompilerCompatibility(): ComboBox<String> {
        // Get available compilers
        val compilerNames = LatexCompiler.values().map { it.displayName }

        val list = ComboBox(compilerNames.toTypedArray())
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply{
            add(JBLabel("Check for compatibility with compiler: "))
            add(list)
        })
        return list
    }

    private fun JPanel.addCheckbox(message: String): JBCheckBox {
        val checkBox = JBCheckBox(message)
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(checkBox)
        })
        return checkBox
    }

    override fun isModified(): Boolean {
        return automaticSoftWraps.isSelected != settings.automaticSoftWraps
                || automaticSecondInlineMathSymbol.isSelected != settings.automaticSecondInlineMathSymbol
                || automaticUpDownBracket.isSelected != settings.automaticUpDownBracket
                || automaticItemInItemize.isSelected != settings.automaticItemInItemize
                || labelDefiningCommands.isModified()
                || automaticQuoteReplacement.selectedIndex != settings.automaticQuoteReplacement.ordinal
                || compilerCompatibility.selectedItem.toString() != settings.compilerCompatibility
    }

    override fun apply() {
        settings.automaticSoftWraps = automaticSoftWraps.isSelected
        settings.automaticSecondInlineMathSymbol = automaticSecondInlineMathSymbol.isSelected
        settings.automaticUpDownBracket = automaticUpDownBracket.isSelected
        settings.automaticItemInItemize = automaticItemInItemize.isSelected
        settings.automaticQuoteReplacement = TexifySettings.QuoteReplacement.values()[automaticQuoteReplacement.selectedIndex]
        settings.compilerCompatibility = compilerCompatibility.selectedItem.toString()
        labelDefiningCommands.apply()
    }

    override fun reset() {
        automaticSoftWraps.isSelected = settings.automaticSoftWraps
        automaticSecondInlineMathSymbol.isSelected = settings.automaticSecondInlineMathSymbol
        automaticUpDownBracket.isSelected = settings.automaticUpDownBracket
        automaticItemInItemize.isSelected = settings.automaticItemInItemize
        labelDefiningCommands.reset()
        automaticQuoteReplacement.selectedIndex = settings.automaticQuoteReplacement.ordinal
        compilerCompatibility.selectedItem = settings.compilerCompatibility
    }
}
