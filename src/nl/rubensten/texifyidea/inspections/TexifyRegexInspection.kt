package nl.rubensten.texifyidea.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import nl.rubensten.texifyidea.insight.InsightGroup
import nl.rubensten.texifyidea.lang.Package
import nl.rubensten.texifyidea.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.jvm.internal.impl.utils.SmartList

/**
 * @author Ruben Schellekens
 */
abstract class TexifyRegexInspection(

        /**
         * The display name of the inspection.
         */
        val inspectionDisplayName: String,

        /**
         * The short name of the inspection (same name as the html info file.
         */
        val myInspectionId: String,

        /**
         * The regex pattern that targets the text for the inspection.
         */
        val pattern: Pattern,

        /**
         * The error message that shows up when you hover over the problem descriptor.
         */
        val errorMessage: (Matcher) -> String,

        /**
         * What to replace in the document.
         */
        val replacement: (Matcher, PsiFile) -> String = { _, _ -> "" },

        /**
         * Fetches different groups from a matcher.
         */
        val groupFetcher: (Matcher) -> List<String> = { listOf() },

        /**
         * The range in the found pattern that must be replaced.
         */
        val replacementRange: (Matcher) -> IntRange = { it.start()..it.end() },

        /**
         * The highlight level of the problem, WEAK_WARNING by default.
         */
        val highlight: ProblemHighlightType = ProblemHighlightType.WEAK_WARNING,

        /**
         * Name of the quick fix.
         */
        val quickFixName: (Matcher) -> String = { "Do fix pls" },

        /**
         * `true` when the inspection is in mathmode, `false` (default) when not in math mode.
         */
        val mathMode: Boolean = false,

        /**
         * Predicate that if `true`, cancels the inspection.
         */
        val cancelIf: (Matcher, PsiFile) -> Boolean = { _, _ -> false },

        /**
         * Provides the text ranges that mark the squiggly warning thingies.
         */
        val highlightRange: (Matcher) -> TextRange = { TextRange(it.start(), it.end()) },

        /**
         * In which inspection group the inspection lies.
         */
        val group: InsightGroup = InsightGroup.LATEX

) : TexifyInspectionBase() {

    companion object {

        /**
         * Get the IntRange that spans the group with the given id.
         */
        fun Matcher.groupRange(groupId: Int): IntRange = start(groupId)..end(groupId)

        /**
         * Checks if the matched element is a child of a certain PsiElement.
         */
        inline fun <reified T : PsiElement> isInElement(matcher: Matcher, file: PsiFile): Boolean {
            val element = file.findElementAt(matcher.start()) ?: return false
            return element.hasParent(T::class)
        }
    }

    override fun getDisplayName() = inspectionDisplayName

    override fun getInspectionId() = myInspectionId

    override fun getInspectionGroup() = group

    override fun checkContext(element: PsiElement) = true

    override fun inspectFile(file: PsiFile, manager: InspectionManager, isOntheFly: Boolean): MutableList<ProblemDescriptor> {
        // Find all patterns.
        val text = file.text
        val matcher = pattern.matcher(text)


        // todo
        if (!isOntheFly && runForWholeFile()) {
            // todo
            val replacementRanges = arrayListOf<IntRange>()
            val replacements = arrayListOf<String>()

            // Then the user is doing a 'fix all problems in file': because we are inspecting the file without doing it on the fly
            // Find all patterns.
            while (matcher.find()) {
                // Pre-checks.
                if (cancelIf(matcher, file)) {
                    continue
                }

                val groups = groupFetcher(matcher)
                val textRange = highlightRange(matcher)
                val range = replacementRange(matcher)
                val error = errorMessage(matcher)
                val quickFix = quickFixName(matcher)
                val replacementContent = replacement(matcher, file)

                // Correct context.
                val element = file.findElementAt(matcher.start()) ?: continue
                if (!checkContext(matcher, element)) {
                    continue
                }

                replacementRanges.add(range)
                replacements.add(replacementContent)
            }

            val problemDescriptor = manager.createProblemDescriptor(
                    file,
                    null as TextRange?,
                    errorMessage(matcher),
                    highlight,
                    isOntheFly,
                    RegexFix(
                            quickFixName(matcher),
                            replacements,
                            replacementRanges,
                            groupFetcher(matcher),
                            this::applyFixes
                    )
                    )

            return mutableListOf(problemDescriptor)

        } else {
            val descriptors = SmartList<ProblemDescriptor>()

            while (matcher.find()) {
                // Pre-checks.
                if (cancelIf(matcher, file)) {
                    continue
                }

                val groups = groupFetcher(matcher)
                val textRange = highlightRange(matcher)
                val range = replacementRange(matcher)
                val error = errorMessage(matcher)
                val quickFix = quickFixName(matcher)
                val replacementContent = replacement(matcher, file)

                // Correct context.
                val element = file.findElementAt(matcher.start()) ?: continue
                if (!checkContext(matcher, element)) {
                    continue
                }

                descriptors.add(manager.createProblemDescriptor(
                        file,
                        textRange,
                        error,
                        highlight,
                        isOntheFly,
                        RegexFix(
                                quickFix,
                                arrayListOf(replacementContent),
                                arrayListOf(range),
                                groups,
                                this::applyFixes
                        )
                ))
            }

            return descriptors
        }
    }

    /**
     * Checks if the element is in the correct context.
     *
     * By default checks for math mode.
     *
     * @return `true` if the inspection is allowed in the context, `false` otherwise.
     */
    open fun checkContext(matcher: Matcher, element: PsiElement): Boolean {
        if (element.isComment()) {
            return false
        }

        return mathMode == element.inMathContext() && checkContext(element)
    }

    /**
     * Replaces all text in the replacementRange by the correct replacement.
     *
     * @param groups todo
     */
    open fun applyFix(project: Project, descriptor: ProblemDescriptor, replacementRange: IntRange, replacement: String, groups: List<String>) {
        val file = descriptor.psiElement as PsiFile
        val document = file.document() ?: return

        document.replaceString(replacementRange.start, replacementRange.endInclusive, replacement)
    }

    /**
     * Replaces all text for all replacementRanges by the correct replacements.
     *
     * todo now inspections cannot override applyfix
     */
    open fun applyFixes(project: Project, descriptor: ProblemDescriptor, replacementRanges: List<IntRange>, replacements: List<String>, groups: List<String>) {
        assert(replacementRanges.size == replacements.size)

        var accumulatedDisplacement = 0

        for (i in 0 until replacements.size) {
            val replacementRange = replacementRanges[i]
            val replacement = replacements[i]

            val file = descriptor.psiElement as PsiFile
            val document = file.document() ?: return

            document.replaceString(replacementRange.start + accumulatedDisplacement, replacementRange.endInclusive + accumulatedDisplacement, replacement)

            accumulatedDisplacement += replacement.length - (replacementRange.endInclusive - replacementRange.start)
        }
    }

    /**
     * @author Ruben Schellekens
     */
    private class RegexFix(
            val fixName: String,
            val replacements: List<String>,
            val replacementRanges: List<IntRange>,
            val groups: List<String>,
            val fixFunction: (Project, ProblemDescriptor, List<IntRange>, List<String>, List<String>) -> Unit
    ) : LocalQuickFix {

        override fun getFamilyName(): String = fixName

        override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
            fixFunction(project, problemDescriptor, replacementRanges, replacements, groups)
        }
    }
}