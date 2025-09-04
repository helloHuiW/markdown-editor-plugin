package com.markdown.editor.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.markdown.editor.editor.MarkdownFileEditor;
import com.markdown.editor.file.MarkdownFileType;
import org.jetbrains.annotations.NotNull;

/**
 * 切换预览显示操作
 */
public class TogglePreviewAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;
        
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor fileEditor = editorManager.getSelectedEditor(file);
        
        if (fileEditor instanceof MarkdownFileEditor) {
            MarkdownFileEditor markdownEditor = (MarkdownFileEditor) fileEditor;
            // 切换预览显示状态
            togglePreviewVisibility(markdownEditor);
        }
    }
    
    private void togglePreviewVisibility(MarkdownFileEditor editor) {
        // 实现预览面板的显示/隐藏切换
        // 这里可以通过调整分割面板的比例来实现
        editor.getPreviewPanel().getComponent().setVisible(
            !editor.getPreviewPanel().getComponent().isVisible()
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        
        boolean enabled = project != null && 
                         file != null && 
                         (file.getFileType() instanceof MarkdownFileType ||
                          isMarkdownFile(file));
        
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
    }
    
    private boolean isMarkdownFile(VirtualFile file) {
        String extension = file.getExtension();
        return extension != null && 
               (extension.equals("md") || 
                extension.equals("markdown") || 
                extension.equals("mdown") || 
                extension.equals("mkd"));
    }
}
