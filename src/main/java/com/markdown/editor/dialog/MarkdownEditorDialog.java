package com.markdown.editor.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBScrollPane;
import com.markdown.editor.preview.MarkdownPreviewPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Markdown编辑器弹窗对话框
 * 包含编辑和预览tab，支持文件打开和保存
 */
public class MarkdownEditorDialog extends DialogWrapper {
    private final Project project;
    private JBTabbedPane tabbedPane;
    private Editor editor;
    private MarkdownPreviewPanel previewPanel;
    private Document document;
    private VirtualFile currentFile;
    private JLabel statusLabel;
    
    public MarkdownEditorDialog(@NotNull Project project) {
        super(project, true);
        this.project = project;
        setTitle("Markdown编辑器");
        setSize(1200, 800);
        init();
        setupDocumentListener();
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建工具栏
        JToolBar toolBar = createToolBar();
        mainPanel.add(toolBar, BorderLayout.NORTH);
        
        // 创建tab面板
        tabbedPane = new JBTabbedPane();
        
        // 编辑器tab
        JPanel editorPanel = createEditorPanel();
        tabbedPane.addTab("编辑", editorPanel);
        
        // 预览tab
        previewPanel = new MarkdownPreviewPanel(project);
        tabbedPane.addTab("预览", previewPanel.getComponent());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // 状态栏
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 新建按钮
        JButton newButton = new JButton("新建");
        newButton.addActionListener(e -> newFile());
        toolBar.add(newButton);
        
        // 打开按钮
        JButton openButton = new JButton("打开");
        openButton.addActionListener(e -> openFile());
        toolBar.add(openButton);
        
        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveFile());
        toolBar.add(saveButton);
        
        // 另存为按钮
        JButton saveAsButton = new JButton("另存为");
        saveAsButton.addActionListener(e -> saveAsFile());
        toolBar.add(saveAsButton);
        
        
        return toolBar;
    }
    
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 创建编辑器
        EditorFactory editorFactory = EditorFactory.getInstance();
        document = editorFactory.createDocument("");
        editor = editorFactory.createEditor(document, project, FileTypes.PLAIN_TEXT, false);
        
        // 配置编辑器
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setRightMarginShown(true);
        settings.setRightMargin(80);
        settings.setWhitespacesShown(false);
        settings.setIndentGuidesShown(true);
        
        JBScrollPane scrollPane = new JBScrollPane(editor.getComponent());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupDocumentListener() {
        if (document != null) {
            document.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    // 延迟更新预览
                    ApplicationManager.getApplication().invokeLater(() -> {
                        updatePreview();
                        updateStatus("文档已修改");
                    });
                }
            });
        }
    }
    
    private void updatePreview() {
        if (document != null && previewPanel != null) {
            String content = document.getText();
            previewPanel.updateContent(content);
        }
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    private void newFile() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.setText("# 新的Markdown文档\n\n开始编写您的内容...\n");
            currentFile = null;
            updateStatus("新建文档");
            updatePreview();
        });
    }
    
    private void openFile() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withFileFilter(file -> {
                    String extension = file.getExtension();
                    return extension != null && (extension.equals("md") || extension.equals("markdown") || 
                                               extension.equals("txt") || extension.equals("mdown"));
                })
                .withTitle("选择Markdown文件")
                .withDescription("选择要打开的Markdown文件");
        
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
        if (files.length > 0) {
            VirtualFile file = files[0];
            try {
                String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.setText(content);
                    currentFile = file;
                    updateStatus("已打开: " + file.getName());
                    updatePreview();
                });
            } catch (IOException e) {
                Messages.showErrorDialog(project, "打开文件失败: " + e.getMessage(), "错误");
            }
        }
    }
    
    private void saveFile() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            saveAsFile();
        }
    }
    
    private void saveAsFile() {
        FileSaverDescriptor descriptor = new FileSaverDescriptor("保存Markdown文件", "选择保存位置", "md", "markdown", "txt");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
        
        var fileWrapper = dialog.save(project.getBaseDir(), "untitled.md");
        if (fileWrapper != null) {
            VirtualFile file = fileWrapper.getVirtualFile(true);
            if (file != null) {
                saveToFile(file);
                currentFile = file;
            }
        }
    }
    
    private void saveToFile(VirtualFile file) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                String content = document.getText();
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                updateStatus("已保存: " + file.getName());
            } catch (IOException e) {
                Messages.showErrorDialog(project, "保存文件失败: " + e.getMessage(), "错误");
            }
        });
    }
    
    @Override
    protected void dispose() {
        if (editor != null && !editor.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(editor);
        }
        if (previewPanel != null) {
            previewPanel.dispose();
        }
        super.dispose();
    }
    
    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction()};
    }
    
    protected String getCancelButtonText() {
        return "关闭";
    }
}
