// This is a generated file. Not intended for manual editing.
package nl.rubensten.texifyidea.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static nl.rubensten.texifyidea.psi.LatexTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import nl.rubensten.texifyidea.psi.*;

public class LatexOptionalParamImpl extends ASTWrapperPsiElement implements LatexOptionalParam {

  public LatexOptionalParamImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull LatexVisitor visitor) {
    visitor.visitOptionalParam(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LatexVisitor) accept((LatexVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public LatexOpenGroup getOpenGroup() {
    return findNotNullChildByClass(LatexOpenGroup.class);
  }

}
