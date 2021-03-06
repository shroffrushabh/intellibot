package com.millennialmedia.intellibot.psi.element;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyClass;
import com.millennialmedia.intellibot.psi.RobotFeatureFileType;
import com.millennialmedia.intellibot.psi.RobotLanguage;
import com.millennialmedia.intellibot.psi.ref.PythonResolver;
import com.millennialmedia.intellibot.psi.ref.RobotPythonClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Stephen Abrams
 */
public class RobotFileImpl extends PsiFileBase implements RobotFile, KeywordFile {

    public RobotFileImpl(FileViewProvider viewProvider) {
        super(viewProvider, RobotLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return RobotFeatureFileType.getInstance();
    }

    @Override
    public Collection<String> getKeywords() {
        List<String> result = new ArrayList<String>();
        for (PsiElement child : getChildren()) {
            if (child instanceof Heading) {
                if (((Heading) child).containsKeywordDefinitions()) {
                    for (PsiElement headingChild : child.getChildren()) {
                        if (headingChild instanceof KeywordDefinition)
                            result.add(((KeywordDefinition) headingChild).getPresentableText());
                    }
                }
            }
        }
        return result;
    }

    @Override
    @NotNull
    public Collection<KeywordFile> getImportedFiles() {
        List<KeywordFile> files = new ArrayList<KeywordFile>();
        Collection<Import> imports = PsiTreeUtil.collectElementsOfType(this, Import.class);
        for (Import imp : imports) {
            if (imp.isResource()) {
                Argument argument = PsiTreeUtil.findChildOfType(imp, Argument.class);
                if (argument != null) {
                    PsiElement resolution = resolveImport(argument);
                    if (resolution instanceof KeywordFile) {
                        files.add((KeywordFile) resolution);
                    }
                }
            } else if (imp.isLibrary()) {
                Argument argument = PsiTreeUtil.findChildOfType(imp, Argument.class);
                if (argument != null) {
                    PyClass resolution = PythonResolver.cast(resolveImport(argument));
                    if (resolution != null) {
                        files.add(new RobotPythonClass(argument.getPresentableText(), resolution));
                    }
                }
            }
        }
        return files;
    }

    @Nullable
    private PsiElement resolveImport(@NotNull Argument argument) {
        PsiReference reference = argument.getReference();
        if (reference != null) {
            return reference.resolve();
        }
        return null;
    }
}
