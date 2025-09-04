package com.markdown.editor.editor;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.markdown.editor.file.MarkdownFileType;
import org.jetbrains.annotations.NotNull;

/**
 * Markdown文件编辑器提供者
 * 负责创建和管理Markdown文件的编辑器实例
 */
public class MarkdownFileEditorProvider implements FileEditorProvider, DumbAware {
    private static final String EDITOR_TYPE_ID = "markdown-editor";
    
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getFileType() instanceof MarkdownFileType || 
               file.getExtension() != null && 
               (file.getExtension().equals("md") || 
                file.getExtension().equals("markdown") ||
                file.getExtension().equals("mdown") ||
                file.getExtension().equals("mkd"));
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new MarkdownFileEditor(project, file);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
