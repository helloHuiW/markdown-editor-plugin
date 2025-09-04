package com.markdown.editor.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.markdown.editor.preview.MarkdownPreviewPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * Markdown文件编辑器
 * 提供分割视图：左侧为文本编辑器，右侧为预览面板
 */
public class MarkdownFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile file;
    private final JBSplitter splitter;
    private final Editor editor;
    private final MarkdownPreviewPanel previewPanel;
    private final Document document;
    
    public MarkdownFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;
        
        // 创建文档和编辑器
        this.document = FileDocumentManager.getInstance().getDocument(file);
        this.editor = createEditor();
        
        // 创建预览面板
        this.previewPanel = new MarkdownPreviewPanel(project);
        
        // 创建分割视图
        this.splitter = new JBSplitter(false, 0.5f);
        setupSplitter();
        
        // 设置文档监听器以实现实时预览
        setupDocumentListener();
        
        // 初始预览内容
        updatePreview();
    }
    
    private Editor createEditor() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Editor editor = editorFactory.createEditor(document, project);
        
        // 配置编辑器设置
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setRightMarginShown(true);
        settings.setRightMargin(80);
        settings.setWhitespacesShown(false);
        settings.setIndentGuidesShown(true);
        
        return editor;
    }
    
    private void setupSplitter() {
        JBScrollPane editorScrollPane = new JBScrollPane(editor.getComponent());
        splitter.setFirstComponent(editorScrollPane);
        splitter.setSecondComponent(previewPanel.getComponent());
        splitter.setDividerWidth(3);
    }
    
    private void setupDocumentListener() {
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                // 延迟更新预览以提高性能
                ApplicationManager.getApplication().invokeLater(() -> updatePreview());
            }
        });
    }
    
    private void updatePreview() {
        if (document != null) {
            String content = document.getText();
            previewPanel.updateContent(content);
        }
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return splitter;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return editor.getContentComponent();
    }

    @NotNull
    @Override
    public String getName() {
        return "Markdown Editor";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // 保存编辑器状态
    }

    @Override
    public boolean isModified() {
        return FileDocumentManager.getInstance().isDocumentUnsaved(document);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // 实现属性变化监听
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // 移除属性变化监听
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        return file;
    }

    @Override
    public void dispose() {
        if (editor != null && !editor.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(editor);
        }
        Disposer.dispose(previewPanel);
    }
    
    // 获取编辑器实例（供外部使用）
    public Editor getEditor() {
        return editor;
    }
    
    // 获取预览面板（供外部使用）
    public MarkdownPreviewPanel getPreviewPanel() {
        return previewPanel;
    }
}
