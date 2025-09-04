package com.markdown.editor.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.intellij.plugins.markdown.lang.MarkdownLanguage;

import javax.swing.*;

/**
 * Markdown文件类型定义
 * 定义了Markdown文件的基本属性和图标
 */
public class MarkdownFileType extends LanguageFileType {
    public static final MarkdownFileType INSTANCE = new MarkdownFileType();
    
    private static final Icon ICON = IconLoader.getIcon("/icons/markdown.svg", MarkdownFileType.class);

    private MarkdownFileType() {
        super(MarkdownLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Markdown Editor File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Markdown文件 - 支持高级编辑和预览功能";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "md";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
