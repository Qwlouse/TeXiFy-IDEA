package nl.rubensten.texifyidea.reference;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import nl.rubensten.texifyidea.TexifyIcons;
import nl.rubensten.texifyidea.completion.handlers.LatexReferenceInsertHandler;
import nl.rubensten.texifyidea.lang.LatexCommand;
import nl.rubensten.texifyidea.psi.BibtexId;
import nl.rubensten.texifyidea.psi.LatexCommands;
import nl.rubensten.texifyidea.psi.LatexRequiredParam;
import nl.rubensten.texifyidea.settings.LabelingCommandInformation;
import nl.rubensten.texifyidea.settings.TexifySettings;
import nl.rubensten.texifyidea.util.LabelsKt;
import nl.rubensten.texifyidea.util.Magic;
import nl.rubensten.texifyidea.util.StringsKt;
import nl.rubensten.texifyidea.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ruben Schellekens, Sten Wessel
 */
public class LatexLabelReference extends PsiReferenceBase<LatexCommands> implements PsiPolyVariantReference {
    private final String key;

    public LatexLabelReference(@NotNull LatexCommands element, TextRange range) {
        super(element);
        key = range.substring(element.getText());

        // Only show Ctrl+click underline under the reference name
        setRangeInElement(range);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean b) {
        Project project = myElement.getProject();
        final Collection<PsiElement> labels = LabelsKt.findLabels(project, key);
        return labels.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        PsiFile file = myElement.getContainingFile().getOriginalFile();
        Map<String, LabelingCommandInformation> commands = TexifySettings.getInstance().getLabelCommands();

        String command = myElement.getCommandToken().getText();

        // add bibreferences to autocompletion for \cite-style commands
        if (Magic.Command.bibliographyReference.contains(command)) {
            return LabelsKt.findBibtexItems(file).stream()
                    .map(bibtexId -> {
                        if (bibtexId != null) {
                            PsiFile containing = bibtexId.getContainingFile();
                            String label;

                            if (bibtexId instanceof BibtexId) {
                                label = StringsKt.substringEnd(bibtexId.getText(), 1);
                            } else if (bibtexId instanceof LatexCommands) {
                                LatexCommands actualCommand = (LatexCommands) bibtexId;
                                List<String> parameters = actualCommand.getRequiredParameters();
                                label = parameters.get(0);
                            } else {
                                return null;
                            }
                            return LookupElementBuilder.create(label)
                                    .bold()
                                    .withInsertHandler(new LatexReferenceInsertHandler())
                                    .withTypeText(containing.getName() + ": " +
                                                    (1 + StringUtil.offsetToLineNumber(containing.getText(), bibtexId.getTextOffset())),
                                            true)
                                    .withIcon(TexifyIcons.DOT_BIB);
                        }
                        return null;
                    }).filter(Objects::nonNull).toArray();
        }
        // add all labels to \ref-styled commands
        else if (Magic.Command.labelReference.contains(command)) {
            return LabelsKt.findLabels(file)
                    .stream()
                    .filter(element -> (element instanceof LatexCommands))
                    .map(element -> (LatexCommands) element)
                    .map(labelingCommand -> {
                        List<String> parameters = labelingCommand.getRequiredParameters();
                        LabelingCommandInformation cmdInfo = commands.get(labelingCommand.getName());
                        if (parameters != null && cmdInfo != null && parameters.size() >= cmdInfo.getPosition()) {
                            String label = parameters.get(cmdInfo.getPosition() - 1);
                            return LookupElementBuilder.create(label)
                                    .bold()
                                    .withInsertHandler(new LatexReferenceInsertHandler())
                                    .withTypeText(labelingCommand.getContainingFile().getName() + ":"
                                            + (1 + StringUtil.offsetToLineNumber(
                                            labelingCommand.getContainingFile().getText(),
                                            labelingCommand.getTextOffset())), true)
                                    .withIcon(TexifyIcons.DOT_LABEL);
                        }
                        else {
                            return null;
                        }
                    }).filter(Objects::nonNull).toArray();
        }
        // if command isn't ref or cite-styled return empty array
        return new Object[]{};
    }
}
