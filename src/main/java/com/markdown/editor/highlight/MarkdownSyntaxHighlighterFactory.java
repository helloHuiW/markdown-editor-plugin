package com.markdown.editor.highlight;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Markdown语法高亮工厂
 * 为Markdown文件提供语法高亮支持
 */
public class MarkdownSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return new MarkdownSyntaxHighlighter();
    }
}
