package com.markdown.editor.toolwindow;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.markdown.editor.file.MarkdownFileType;
import com.markdown.editor.preview.MarkdownPreviewPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Markdown预览工具窗口
 */
public class MarkdownPreviewToolWindow {
    private final Project project;
    private final JPanel mainPanel;
    private MarkdownPreviewPanel previewPanel;
    private final JBLabel statusLabel;
    
    public MarkdownPreviewToolWindow(Project project) {
        this.project = project;
        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.statusLabel = new JBLabel("选择一个Markdown文件以查看预览");
        
        initializeUI();
        setupFileEditorListener();
    }
    
    private void initializeUI() {
        // 状态标签
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        mainPanel.add(statusLabel, BorderLayout.CENTER);
    }
    
    private void setupFileEditorListener() {
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, 
            new FileEditorManagerListener() {
                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    VirtualFile newFile = event.getNewFile();
                    updatePreview(newFile);
                }
            }
        );
        
        // 初始检查当前选中的文件
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        if (selectedFiles.length > 0) {
            updatePreview(selectedFiles[0]);
        }
    }
    
    private void updatePreview(VirtualFile file) {
        if (file == null || !isMarkdownFile(file)) {
            showStatusMessage("选择一个Markdown文件以查看预览");
            return;
        }
        
        try {
            // 移除现有的预览面板
            if (previewPanel != null) {
                mainPanel.remove(previewPanel.getComponent());
                previewPanel.dispose();
            }
            
            // 创建新的预览面板
            previewPanel = new MarkdownPreviewPanel(project);
            
            // 读取文件内容
            String content = new String(file.contentsToByteArray(), file.getCharset());
            previewPanel.updateContent(content);
            
            // 更新UI
            mainPanel.removeAll();
            mainPanel.add(previewPanel.getComponent(), BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();
            
        } catch (Exception e) {
            showStatusMessage("预览错误: " + e.getMessage());
        }
    }
    
    private void showStatusMessage(String message) {
        if (previewPanel != null) {
            mainPanel.remove(previewPanel.getComponent());
            previewPanel.dispose();
            previewPanel = null;
        }
        
        statusLabel.setText(message);
        mainPanel.removeAll();
        mainPanel.add(statusLabel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }
    
    private boolean isMarkdownFile(VirtualFile file) {
        return file.getFileType() instanceof MarkdownFileType ||
               (file.getExtension() != null && 
                (file.getExtension().equals("md") || 
                 file.getExtension().equals("markdown") ||
                 file.getExtension().equals("mdown") ||
                 file.getExtension().equals("mkd")));
    }
    
    public JComponent getContent() {
        return mainPanel;
    }
}
