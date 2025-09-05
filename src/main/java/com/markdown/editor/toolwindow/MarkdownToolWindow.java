package com.markdown.editor.toolwindow;

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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBScrollPane;
import com.markdown.editor.preview.MarkdownPreviewPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Markdown工具窗口
 * 直接在工具窗口内提供编辑和预览功能
 */
public class MarkdownToolWindow implements com.intellij.openapi.Disposable {
    
    
    private final Project project;
    private final JPanel mainPanel;
    private JBTabbedPane tabbedPane;
    private Editor editor;
    private MarkdownPreviewPanel previewPanel;
    private Document document;
    private VirtualFile currentFile;
    private JLabel statusLabel;
    private JLabel filePathLabel;
    private boolean isDocumentModified = false;
    private ToolWindow toolWindow;
    
    // 全局标志位：插件是否正在卸载
    private static volatile boolean isPluginUnloading = false;
    
    // 🚨 全局紧急停止标志 - 用于最紧急的情况
    private static volatile boolean EMERGENCY_STOP_ALL_SAVES = false;
    
    // 🔍 插件卸载检测标志
    private static volatile boolean PLUGIN_IS_UNLOADING = false;
    
    public MarkdownToolWindow(Project project) {
        this.project = project;
        this.mainPanel = new JBPanel<>(new BorderLayout());
        

        initializeUI();
        setupDocumentListener();
        setupVisibilityListener();
        setupPluginUnloadListener();
    }
    
    
    private void initializeUI() {
        // 移除主面板边框
        mainPanel.setBorder(null);
        
        // 创建顶部面板：包含工具栏和文件路径
        JPanel topPanel = new JBPanel<>(new BorderLayout());
        topPanel.setBorder(null);
        
        // 创建工具栏
        JToolBar toolBar = createToolBar();
        topPanel.add(toolBar, BorderLayout.NORTH);
        
        // 创建文件路径显示面板
        JPanel filePathPanel = createFilePathPanel();
        topPanel.add(filePathPanel, BorderLayout.SOUTH);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // 创建Tab面板
        tabbedPane = new JBTabbedPane();
        tabbedPane.setBorder(null); // 移除Tab面板边框
        
        // 编辑器Tab
        JPanel editorPanel = createEditorPanel();
        tabbedPane.addTab("📝 编辑", editorPanel);
        
        // 预览Tab
        previewPanel = new MarkdownPreviewPanel(project);
        tabbedPane.addTab("👁️ 预览", previewPanel.getComponent());
        
        // 添加Tab切换监听器，确保切换到预览时自动刷新
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 1) { // 预览Tab的索引是1
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 强制刷新预览内容，不依赖缓存
                    forceRefreshPreview();
                    updateStatus("预览已刷新");
                });
            }
        });
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // 状态栏
        statusLabel = new JBLabel("就绪 - 开始编写Markdown内容");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        // UI组件都添加完毕后，确保默认选中编辑Tab
        ApplicationManager.getApplication().invokeLater(() -> {
            tabbedPane.setSelectedIndex(0);
            System.out.println("🔄 强制设置编辑Tab为选中状态: " + tabbedPane.getSelectedIndex());
            
            // 然后初始化新文档
            newFile();
        });
    }
    
    private JPanel createFilePathPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        
        // 创建文件路径标签
        filePathLabel = new JBLabel("📄 新建文档 (未保存)");
        filePathLabel.setForeground(new Color(100, 130, 160));
        filePathLabel.setFont(filePathLabel.getFont().deriveFont(Font.PLAIN, 11f));
        
        panel.add(filePathLabel, BorderLayout.WEST);
        
        return panel;
    }
    
    /**
     * 更新文件路径显示
     */
    private void updateFilePathDisplay() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (currentFile != null) {
                String fileName = currentFile.getName();
                String fullPath = currentFile.getPath();
                String displayText = String.format("📄 %s - %s", fileName, fullPath);
                if (isDocumentModified) {
                    displayText = "🔸 " + displayText + " (已修改)";
                }
                filePathLabel.setText(displayText);
                filePathLabel.setToolTipText(fullPath);
            } else {
                String displayText = isDocumentModified ? "🔸 新建文档 (未保存*)" : "📄 新建文档 (未保存)";
                filePathLabel.setText(displayText);
                filePathLabel.setToolTipText("新建的文档，尚未保存到文件");
            }
        });
    }
    
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(null); // 移除工具栏边框
        
        // 新建按钮
        JButton newButton = new JButton("新建");
        newButton.setToolTipText("创建新的Markdown文档");
        newButton.addActionListener(e -> newFile());
        toolBar.add(newButton);
        
        // 打开按钮
        JButton openButton = new JButton("打开");
        openButton.setToolTipText("打开本地Markdown文件");
        openButton.addActionListener(e -> openFile());
        toolBar.add(openButton);
        
        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.setToolTipText("保存当前文档");
        saveButton.addActionListener(e -> saveFile());
        toolBar.add(saveButton);
        
        // 另存为按钮
        JButton saveAsButton = new JButton("另存为");
        saveAsButton.setToolTipText("另存为新文件");
        saveAsButton.addActionListener(e -> saveAsFile());
        toolBar.add(saveAsButton);
        
        
        return toolBar;
    }
    
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(null); // 移除编辑器面板边框
        
        // 创建编辑器
        EditorFactory editorFactory = EditorFactory.getInstance();
        document = editorFactory.createDocument("");
        editor = editorFactory.createEditor(document, project, FileTypes.PLAIN_TEXT, false);
        
        // 确保编辑器可编辑
        if (editor instanceof EditorEx) {
            ((EditorEx) editor).setViewer(false);
        }
        
        // 配置编辑器
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setRightMarginShown(true);
        settings.setRightMargin(80);
        settings.setWhitespacesShown(false);
        settings.setIndentGuidesShown(true);
        
        // 创建格式工具栏
        JToolBar formatToolBar = createFormatToolBar();
        panel.add(formatToolBar, BorderLayout.NORTH);
        
        JBScrollPane scrollPane = new JBScrollPane(editor.getComponent());
        scrollPane.setBorder(null); // 移除编辑器滚动面板边框
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupDocumentListener() {
        if (document != null) {
            document.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    // 🚨🚨🚨 最高优先级：检查紧急停止标志
                    if (EMERGENCY_STOP_ALL_SAVES) {
                        System.out.println("🚨 检测到紧急停止标志，忽略文档变化！");
                        return;
                    }
                    
                    // 🚨 在处理文档变化前检查插件状态
                    if (isPluginUnloading) {
                        System.out.println("🚫 插件正在关闭，忽略文档变化");
                        return;
                    }
                    
                    // 标记文档已修改
                    isDocumentModified = true;
                    System.out.println("📝 文档已修改，设置状态: " + isDocumentModified);
                    
                    // 延迟更新预览和文件路径显示
                    ApplicationManager.getApplication().invokeLater(() -> {
                        // 再次检查状态，防止在UI线程中执行不必要的操作
                        if (isPluginUnloading) {
                            System.out.println("🚫 UI线程中检测到插件正在关闭，跳过更新");
                            return;
                        }
                        
                        updatePreview();
                        updateStatus("文档已修改");
                        updateFilePathDisplay();
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
    
    /**
     * 强制刷新预览，清除缓存并重新渲染
     */
    private void forceRefreshPreview() {
        if (document != null && previewPanel != null) {
            String content = document.getText();
            // 调用预览面板的刷新方法，强制重新渲染
            previewPanel.refresh();
            // 然后更新内容，确保最新内容被渲染
            previewPanel.updateContent(content);
        }
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    private void newFile() {
        System.out.println("🆕 新建文件 - 当前修改状态: " + isDocumentModified);
        
        // 检查当前文档是否有未保存的修改
        if (isDocumentModified) {
            System.out.println("🔔 显示保存确认对话框");
            int choice = Messages.showYesNoCancelDialog(
                project,
                "当前文档有未保存的修改，是否保存？",
                "保存确认",
                "保存",
                "不保存",
                "取消",
                Messages.getQuestionIcon()
            );
            
            switch (choice) {
                case Messages.YES: // 保存
                    saveFile();
                    if (isDocumentModified) {
                        // 如果用户取消了保存，则不创建新文件
                        return;
                    }
                    break;
                case Messages.NO: // 不保存
                    // 继续创建新文件
                    break;
                case Messages.CANCEL: // 取消
                default:
                    return; // 取消操作
            }
        }
        
        // 切换到编辑Tab
        tabbedPane.setSelectedIndex(0);
        
        // 创建新文档内容
        String newContent = "# 新的Markdown文档\n\n开始编写您的内容...\n\n## 标题示例\n\n这是一个**粗体**文本和*斜体*文本的示例。\n\n访问 [百度](www.baidu.com)\n\nGitHub: [链接](github.com/user/repo)\n\n邮箱: contact@example.com\n\n完整链接: https://www.google.com\n\n```java\n// 代码块示例\npublic class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World!\");\n    }\n}\n```\n\n> 这是一个引用块\n\n## 多级列表示例\n\n### 无序列表嵌套\n- 一级项目1\n  - 二级项目1\n    - 三级项目1\n    - 三级项目2\n  - 二级项目2\n- 一级项目2\n\n### 有序列表嵌套\n1. 第一步\n   1. 子步骤1.1\n   2. 子步骤1.2\n      1. 详细步骤1.2.1\n      2. 详细步骤1.2.2\n2. 第二步\n\n### 混合列表嵌套\n1. 有序项目1\n   - 无序子项目1\n   - 无序子项目2\n     1. 有序孙项目1\n     2. 有序孙项目2\n2. 有序项目2\n   - 无序子项目A\n     - 更深层无序项目\n";
        
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.setText(newContent);
            currentFile = null;
            isDocumentModified = false; // 重置修改状态
            updateStatus("新建文档");
            updateFilePathDisplay();
            
            // 只有在预览Tab被选中时才更新预览（避免初始化时的状态混乱）
            if (tabbedPane.getSelectedIndex() == 1) {
                updatePreview();
            }
        });
        
        // 确保焦点在编辑器上
        ApplicationManager.getApplication().invokeLater(() -> {
            if (editor != null) {
                editor.getComponent().requestFocus();
                // 将光标定位到文档开头
                editor.getCaretModel().moveToOffset(0);
            }
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
                    isDocumentModified = false; // 重置修改状态
                    updateStatus("已打开: " + file.getName());
                    updatePreview();
                    updateFilePathDisplay();
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
                updateFilePathDisplay();
            }
        }
    }
    
    private void saveToFile(VirtualFile file) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                String content = document.getText();
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                isDocumentModified = false; // 保存成功后重置修改状态
                updateStatus("已保存: " + file.getName());
                updateFilePathDisplay();
            } catch (IOException e) {
                Messages.showErrorDialog(project, "保存文件失败: " + e.getMessage(), "错误");
            }
        });
    }
    
    public JComponent getContent() {
        return mainPanel;
    }
    
    // 清理资源
    private JToolBar createFormatToolBar() {
        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new BoxLayout(toolBarPanel, BoxLayout.Y_AXIS));
        toolBarPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        
        // 第一行：标题和文本格式
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        row1.add(new JLabel("标题:"));
        row1.add(createSmallFormatButton("H1", "# ", "在行首插入一级标题"));
        row1.add(createSmallFormatButton("H2", "## ", "在行首插入二级标题"));
        row1.add(createSmallFormatButton("H3", "### ", "在行首插入三级标题"));
        
        row1.add(Box.createHorizontalStrut(10));
        row1.add(new JLabel("格式:"));
        row1.add(createSmallWrapButton("B", "**", "**", "粗体"));
        row1.add(createSmallWrapButton("I", "*", "*", "斜体"));
        row1.add(createSmallWrapButton("S", "~~", "~~", "删除线"));
        row1.add(createSmallWrapButton("C", "`", "`", "行内代码"));
        
        // 第二行：列表和插入元素
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        row2.add(new JLabel("列表:"));
        row2.add(createSmallFormatButton("•", "- ", "插入无序列表"));
        row2.add(createSmallFormatButton("1.", "1. ", "插入有序列表"));
        row2.add(createSmallFormatButton(">", "> ", "插入引用"));
        
        row2.add(Box.createHorizontalStrut(10));
        row2.add(new JLabel("插入:"));
        row2.add(createSmallTemplateButton("链接", "[链接文本](URL)", "插入链接"));
        row2.add(createSmallTemplateButton("图片", "![图片描述](图片URL)", "插入图片"));
        row2.add(createSmallTemplateButton("代码", "```\\n代码内容\\n```", "插入代码块"));
        row2.add(createSmallTemplateButton("表格", "|列1|列2|\\n|---|---|\\n|A|B|", "插入表格"));
        row2.add(createSmallTemplateButton("---", "\\n---\\n", "插入分割线"));
        
        toolBarPanel.add(row1);
        toolBarPanel.add(row2);
        
        // 包装在滚动面板中
        JScrollPane scrollPane = new JScrollPane(toolBarPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 70));
        
        // 创建一个工具栏容器
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(null); // 移除格式工具栏边框
        toolBar.setLayout(new BorderLayout());
        toolBar.add(scrollPane, BorderLayout.CENTER);
        
        return toolBar;
    }
    
    private JButton createSmallFormatButton(String text, String format, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(button.getFont().deriveFont(10f));
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setPreferredSize(new Dimension(Math.max(40, text.length() * 8 + 16), 24));
        button.addActionListener(e -> {
            System.out.println("格式按钮点击: " + format); // 调试日志
            insertAtLineStart(format);
        });
        return button;
    }
    
    private JButton createSmallWrapButton(String text, String startWrap, String endWrap, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(button.getFont().deriveFont(10f));
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setPreferredSize(new Dimension(Math.max(30, text.length() * 8 + 16), 24));
        button.addActionListener(e -> {
            System.out.println("包装按钮点击: " + startWrap + "..." + endWrap); // 调试日志
            wrapSelectedText(startWrap, endWrap);
        });
        return button;
    }
    
    private JButton createSmallTemplateButton(String text, String template, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(button.getFont().deriveFont(10f));
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setPreferredSize(new Dimension(Math.max(45, text.length() * 8 + 16), 24));
        button.addActionListener(e -> {
            System.out.println("模板按钮点击: " + template); // 调试日志
            insertTemplate(template);
        });
        return button;
    }
    
    private void insertAtLineStart(String format) {
        if (editor == null || document == null) {
            System.out.println("编辑器或文档为空");
            updateStatus("编辑器未就绪");
            return;
        }
        
        try {
            // 使用WriteCommandAction确保操作在正确的线程中执行
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    // 强制刷新编辑器状态
                    editor.getContentComponent().requestFocusInWindow();
                    
                    int caretOffset = editor.getCaretModel().getOffset();
                    int lineNumber = document.getLineNumber(caretOffset);
                    int lineStartOffset = document.getLineStartOffset(lineNumber);
                    
                    // 检查行首是否已经有相同格式
                    String lineText = "";
                    if (lineStartOffset < document.getTextLength()) {
                        int endOffset = Math.min(lineStartOffset + format.length(), document.getTextLength());
                        lineText = document.getText().substring(lineStartOffset, endOffset);
                    }
                    
                    if (!lineText.equals(format)) {
                        document.insertString(lineStartOffset, format);
                        System.out.println("✅ 成功插入格式: " + format);
                        
                        // 移动光标到格式后面
                        editor.getCaretModel().moveToOffset(lineStartOffset + format.length());
                        
                        // 强制更新预览
                        updatePreview();
                        updateStatus("✅ 已插入格式: " + format.trim());
                    } else {
                        updateStatus("ℹ️ 格式已存在: " + format.trim());
                    }
                } catch (Exception ex) {
                    System.out.println("❌ 插入失败: " + ex.getMessage());
                    ex.printStackTrace();
                    updateStatus("❌ 插入失败: " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            System.out.println("❌ 操作失败: " + ex.getMessage());
            updateStatus("❌ 操作失败: " + ex.getMessage());
        }
    }
    
    private void wrapSelectedText(String startWrap, String endWrap) {
        if (editor == null || document == null) {
            System.out.println("编辑器或文档为空");
            updateStatus("编辑器未就绪");
            return;
        }
        
        try {
            // 使用WriteCommandAction确保操作在正确的线程中执行
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    // 强制刷新编辑器状态
                    editor.getContentComponent().requestFocusInWindow();
                    
                    String selectedText = editor.getSelectionModel().getSelectedText();
                    int selectionStart = editor.getSelectionModel().getSelectionStart();
                    int selectionEnd = editor.getSelectionModel().getSelectionEnd();
                    
                    if (selectedText != null && !selectedText.isEmpty()) {
                        // 包装选中的文本
                        String wrappedText = startWrap + selectedText + endWrap;
                        document.replaceString(selectionStart, selectionEnd, wrappedText);
                        
                        // 选中包装后的内容
                        editor.getSelectionModel().setSelection(selectionStart, 
                            selectionStart + wrappedText.length());
                        
                        System.out.println("✅ 成功包装文本: " + wrappedText);
                        updateStatus("✅ 已应用格式: " + startWrap + "文本" + endWrap);
                    } else {
                        // 没有选中文本，在光标位置插入格式
                        int caretOffset = editor.getCaretModel().getOffset();
                        String placeholder = startWrap + "文本" + endWrap;
                        document.insertString(caretOffset, placeholder);
                        
                        // 选中"文本"部分
                        editor.getSelectionModel().setSelection(caretOffset + startWrap.length(), 
                            caretOffset + startWrap.length() + 2);
                        
                        System.out.println("✅ 成功插入模板: " + placeholder);
                        updateStatus("✅ 已插入格式模板: " + startWrap + "文本" + endWrap);
                    }
                    
                    // 强制更新预览
                    updatePreview();
                } catch (Exception ex) {
                    System.out.println("❌ 包装文本失败: " + ex.getMessage());
                    ex.printStackTrace();
                    updateStatus("❌ 插入失败: " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            System.out.println("❌ 操作失败: " + ex.getMessage());
            updateStatus("❌ 操作失败: " + ex.getMessage());
        }
    }
    
    private void insertTemplate(String template) {
        if (editor == null || document == null) {
            System.out.println("编辑器或文档为空");
            updateStatus("编辑器未就绪");
            return;
        }
        
        try {
            // 使用WriteCommandAction确保操作在正确的线程中执行
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    // 强制刷新编辑器状态
                    editor.getContentComponent().requestFocusInWindow();
                    
                    int caretOffset = editor.getCaretModel().getOffset();
                    
                    // 处理模板中的换行符
                    String processedTemplate = template.replace("\\n", "\n");
                    
                    document.insertString(caretOffset, processedTemplate);
                    
                    // 移动光标到插入内容的末尾
                    editor.getCaretModel().moveToOffset(caretOffset + processedTemplate.length());
                    
                    System.out.println("✅ 成功插入模板: " + processedTemplate);
                    updateStatus("✅ 已插入模板: " + template.split("\\\\n")[0]);
                    
                    // 强制更新预览
                    updatePreview();
                } catch (Exception ex) {
                    System.out.println("❌ 插入模板失败: " + ex.getMessage());
                    ex.printStackTrace();
                    updateStatus("❌ 插入失败: " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            System.out.println("❌ 操作失败: " + ex.getMessage());
            updateStatus("❌ 操作失败: " + ex.getMessage());
        }
    }
    
    /**
     * 设置插件卸载监听器
     */
    private void setupPluginUnloadListener() {
        try {
            // 使用项目级别的MessageBus来监听插件卸载事件
            project.getMessageBus().connect(this)
                .subscribe(com.intellij.ide.plugins.DynamicPluginListener.TOPIC, 
                    new com.intellij.ide.plugins.DynamicPluginListener() {
                        @Override
                        public void beforePluginUnload(@org.jetbrains.annotations.NotNull 
                                                     com.intellij.ide.plugins.IdeaPluginDescriptor pluginDescriptor, 
                                                     boolean isUpdate) {
                            // 检查是否是当前插件
                            String pluginId = pluginDescriptor.getPluginId().getIdString();
                            System.out.println("🔍 插件即将卸载: " + pluginId);
                            
                            if (pluginId.contains("markdown-editor") || 
                                pluginId.contains("markdown") ||
                                pluginDescriptor.getName().toLowerCase().contains("markdown")) {
                                
                                System.out.println("🚨 检测到Markdown插件即将卸载: " + pluginId);
                                
                                // 设置全局卸载标志
                                PLUGIN_IS_UNLOADING = true;
                                EMERGENCY_STOP_ALL_SAVES = true;
                                isPluginUnloading = true;
                                
                                // 立即清除状态，防止保存操作
                                if (MarkdownToolWindow.this.currentFile != null) {
                                    MarkdownToolWindow.this.currentFile = null;
                                }
                                MarkdownToolWindow.this.isDocumentModified = false;
                                
                                System.out.println("🚨 已设置插件卸载标志，阻止所有保存操作");
                            }
                        }
                        
                        @Override
                        public void pluginUnloaded(@org.jetbrains.annotations.NotNull 
                                                 com.intellij.ide.plugins.IdeaPluginDescriptor pluginDescriptor, 
                                                 boolean isUpdate) {
                            String pluginId = pluginDescriptor.getPluginId().getIdString();
                            System.out.println("🔍 插件已卸载: " + pluginId);
                        }
                    });
            
            System.out.println("✅ 插件卸载监听器设置成功");
            
        } catch (Exception ex) {
            System.err.println("❌ 设置插件卸载监听器失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 设置全局插件卸载状态（供外部调用，如插件卸载时）
     */
    public static void setPluginUnloading(boolean unloading) {
        isPluginUnloading = unloading;
        if (unloading) {
            System.out.println("🚫 设置全局插件卸载状态，所有实例将跳过自动保存");
            // 🚨🚨🚨 插件卸载时设置紧急停止标志
            EMERGENCY_STOP_ALL_SAVES = true;
            PLUGIN_IS_UNLOADING = true;
            System.out.println("🚨 插件卸载，设置紧急停止标志阻止所有保存操作");
        }
    }
    
    /**
     * 检测是否为插件卸载操作
     */
    private boolean isPluginUninstallOperation() {
        try {
            // 🔍 方法1: 检查插件卸载监听器设置的标志
            if (PLUGIN_IS_UNLOADING) {
                System.out.println("🔍 通过插件监听器检测到卸载操作");
                return true;
            }
            
            // 🔍 方法2: 检查线程名称
            String threadName = Thread.currentThread().getName();
            System.out.println("🔍 当前线程名称: " + threadName);
            
            // 检查是否为插件卸载相关的线程
            if (threadName.toLowerCase().contains("plugin") && 
                (threadName.toLowerCase().contains("unload") || 
                 threadName.toLowerCase().contains("uninstall") ||
                 threadName.toLowerCase().contains("disable"))) {
                System.out.println("🔍 检测到插件卸载线程: " + threadName);
                return true;
            }
            
            // 🔍 方法3: 检查调用栈
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                
                // 检查IntelliJ插件管理相关的类
                if (className.contains("DynamicPlugins") || 
                    className.contains("PluginManagerCore") ||
                    className.contains("PluginInstaller") ||
                    className.contains("IdeaPluginDescriptor")) {
                    
                    if (methodName.contains("unload") || 
                        methodName.contains("disable") ||
                        methodName.contains("uninstall")) {
                        System.out.println("🔍 在调用栈中发现插件卸载操作: " + className + "." + methodName);
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (Exception ex) {
            System.err.println("❌ 检测插件卸载操作时发生异常: " + ex.getMessage());
            return false; // 出现异常时保守处理，不认为是卸载
        }
    }
    
    /**
     * 设置可见性监听器
     */
    private void setupVisibilityListener() {
        mainPanel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                System.out.println("🔄 层次事件触发 - isShowing: " + mainPanel.isShowing() + 
                                 ", isPluginUnloading: " + isPluginUnloading);

                // 🚨🚨🚨 最高优先级：检查紧急停止标志（仅在插件卸载时设置）
                if (EMERGENCY_STOP_ALL_SAVES) {
                    System.out.println("🚨 检测到紧急停止标志，强制跳过所有保存操作！");
                    return;
                }
                
                if (!mainPanel.isShowing()) {
                    // 面板变为不可见且不是插件关闭时才触发自动保存
                    System.out.println("🔄 检测到Markdown工具窗口变为不可见，检查是否需要自动保存");
                    
                    // 添加额外的检查：确保不是因为IDE关闭或项目关闭导致的不可见
                    if (isNormalHideOperation()) {
                        System.out.println("✅ 确认为正常隐藏操作，触发自动保存");
                        autoSaveOnClose();
                    } else {
                        System.out.println("ℹ️ 检测到异常隐藏操作（可能是IDE关闭），跳过自动保存");
                    }
                } else {
                    System.out.println("ℹ️ 不符合自动保存条件，跳过");
                }
            }
        });
    }
    
    /**
     * 设置工具窗口引用并添加监听器
     */
    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        setupTabChangeListener();
    }
    
    /**
     * 设置标签页切换监听器
     */
    private void setupTabChangeListener() {
        if (tabbedPane != null) {
            tabbedPane.addChangeListener(e -> {
                // 标签页切换时刷新预览（如果切换到预览Tab）
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex == 1) { // 1是预览Tab
                    System.out.println("🔄 切换到预览Tab，刷新预览内容");
                    // 这里可以添加刷新预览的逻辑，但不保存文件
                    if (previewPanel != null) {
                        String currentContent = "";
                        if (editor != null && !editor.isDisposed()) {
                            currentContent = editor.getDocument().getText();
                        }
                        previewPanel.updateContent(currentContent);
                    }
                }
            });
        }
    }
    
    /**
     * 判断是否为正常的隐藏操作（非IDE关闭或插件卸载）
     */
    private boolean isNormalHideOperation() {
        try {
            
            // 检查工具窗口是否仍然有效
            if (toolWindow == null || !toolWindow.isAvailable()) {
                return false;
            }
            
            // 如果所有检查都通过，认为是正常的隐藏操作
            return true;
            
        } catch (Exception ex) {
            System.err.println("❌ 检查隐藏操作状态时发生错误: " + ex.getMessage());
            return false; // 出现异常时保守处理，不触发保存
        }
    }
    
    /**
     * 判断dispose时是否应该自动保存
     */
    private boolean shouldAutoSaveOnDispose() {
        try {
         
            
            // 📌 检查全局插件卸载状态
            if (isPluginUnloading) {
                System.out.println("🚫 插件正在卸载，全局跳过保存");
                return false;
            }
            
            // 检查当前编辑器和文档状态
            if (editor == null || editor.isDisposed()) {
                System.out.println("ℹ️ 编辑器已释放，跳过保存");
                return false;
            }
            
            // 检查是否有实际的修改内容需要保存
            if (!isDocumentModified) {
                System.out.println("ℹ️ 文档未修改，跳过保存");
                return false;
            }
            
            // 检查内容是否为空
            String content = editor.getDocument().getText();
            if (content.trim().isEmpty()) {
                System.out.println("ℹ️ 内容为空，跳过保存");
                return false;
            }
            
            // 所有检查通过，可以保存
            return true;
            
        } catch (Exception ex) {
            System.err.println("❌ 检查保存条件时发生错误: " + ex.getMessage());
            return false; // 出现异常时保守处理，不保存
        }
    }
    
    /**
     * 工具窗口隐藏时自动保存（供外部调用）
     */
    public void autoSaveOnHide() {
        if ( !isPluginUnloading && isNormalHideOperation()) {
            autoSaveOnClose();
        } else {
            System.out.println("ℹ️ 检测到插件正在关闭或异常状态，跳过外部触发的自动保存");
        }
    }
    
    /**
     * 工具窗口关闭时自动保存
     */
    private void autoSaveOnClose() {
        try {
            System.out.println("💾 工具窗口关闭，检查是否需要自动保存");
            
            // 🚨🚨🚨 最高优先级：检查紧急停止标志（仅在插件卸载时设置）
            if (EMERGENCY_STOP_ALL_SAVES) {
                System.out.println("🚨 检测到紧急停止标志，强制跳过自动保存！");
                return;
            }
            
            // 获取当前编辑器内容
            String content = "";
            if (editor != null && !editor.isDisposed()) {
                content = editor.getDocument().getText();
            } else {
                System.out.println("ℹ️ 编辑器不可用，跳过自动保存");
                return;
            }
            
            // 检查是否有内容需要保存
            if (content.trim().isEmpty()) {
                System.out.println("ℹ️ 编辑器内容为空，跳过自动保存");
                return;
            }
            
            // 检查文档是否真的有修改
            if (!isDocumentModified) {
                System.out.println("ℹ️ 文档未修改，跳过自动保存");
                return;
            }
            
            // 🚨 最后一次检查：在执行任何保存操作前再次确认状态
            if (isPluginUnloading) {
                System.out.println("🚫 最终检查发现插件正在关闭，强制终止保存操作");
                return;
            }
            
            if (currentFile != null && isDocumentModified) {
                // 情况1: 已有文件且已修改 - 直接保存
                System.out.println("📝 检测到已有文件已修改，执行自动保存: " + currentFile.getName());
                autoSaveExistingFile(content);
            } else if (currentFile == null && isDocumentModified) {
                // 情况2: 新建文档且有内容 - 提示用户保存
                System.out.println("📝 检测到新建文档有内容，提示保存");
                autoSaveNewDocument(content);
            } else if (!isDocumentModified) {
                System.out.println("ℹ️ 文件未修改，跳过自动保存");
            }
        } catch (Exception ex) {
            System.err.println("❌ 自动保存过程中发生错误: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 自动保存已存在的文件
     */
    private void autoSaveExistingFile(String content) {
        // 🚨🚨🚨 最高优先级：检查紧急停止标志
        if (EMERGENCY_STOP_ALL_SAVES) {
            System.out.println("🚨 检测到紧急停止标志，强制跳过文件保存！");
            return;
        }
        
        // 🚨 在执行写操作前最后一次检查状态
        if (isPluginUnloading) {
            System.out.println("🚫 写操作前检查发现插件正在关闭，取消保存");
            return;
        }
        
        final String finalContent = content;
        final VirtualFile finalCurrentFile = currentFile;
        
        try {
            ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        // 🚨🚨🚨 最高优先级：在实际写入前检查紧急停止标志
                        if (EMERGENCY_STOP_ALL_SAVES) {
                            System.out.println("🚨 写入前检测到紧急停止标志，强制取消写入！");
                            return;
                        }
                        
                        // 🚨 在实际写入前再次检查状态
                        if (isPluginUnloading) {
                            System.out.println("🚫 写入前发现插件正在关闭，取消写入");
                            return;
                        }
                        
                        finalCurrentFile.setBinaryContent(finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        isDocumentModified = false;
                        System.out.println("✅ 自动保存成功: " + finalCurrentFile.getName());
                        updateStatus("✅ 自动保存成功: " + finalCurrentFile.getName());
                    } catch (Exception ex) {
                        System.err.println("❌ 自动保存失败: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                System.err.println("❌ 自动保存写入操作失败: " + ex.getMessage());
            }
        });
        } catch (Exception ex) {
            System.err.println("❌ 调用写入操作时发生错误: " + ex.getMessage());
        }
    }
    
    /**
     * 自动保存新建文档
     */
    private void autoSaveNewDocument(String content) {
        // 🚨🚨🚨 最高优先级：检查紧急停止标志
        if (EMERGENCY_STOP_ALL_SAVES) {
            System.out.println("🚨 检测到紧急停止标志，强制跳过新文档保存！");
            return;
        }
        
        // 检查插件状态
        if (isPluginUnloading) {
            System.out.println("🚫 插件正在关闭，跳过保存对话框");
            return;
        }
        
        try {
            ApplicationManager.getApplication().invokeLater(() -> {
                // 在UI线程中再次检查状态
                if (isPluginUnloading) {
                    System.out.println("🚫 应用程序或插件正在关闭，跳过保存对话框");
                    return;
                }
      
                try {
                    int choice = Messages.showYesNoDialog(
                        project,
                        "检测到未保存的新建文档，是否要保存？",
                        "自动保存提示",
                        "保存",
                        "不保存",
                        Messages.getQuestionIcon()
                    );
                    
                    if (choice == Messages.YES) {
                        // 用户选择保存 - 打开保存对话框
                        System.out.println("📁 用户选择保存新建文档，打开保存对话框");
                        saveAsFileWithContent(content);
                    } else {
                        System.out.println("ℹ️ 用户选择不保存新建文档");
                    }
                } catch (Exception ex) {
                    System.err.println("❌ 显示保存对话框时发生错误: " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            System.err.println("❌ 调度UI任务时发生错误: " + ex.getMessage());
        }
    }
    
    /**
     * 使用指定内容另存为文件
     */
    private void saveAsFileWithContent(String content) {
        FileSaverDescriptor descriptor = new FileSaverDescriptor("保存Markdown文件", "选择保存位置", "md", "markdown", "txt");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
        
        var fileWrapper = dialog.save(project.getBaseDir(), "untitled.md");
        if (fileWrapper != null) {
            try {
                VirtualFile savedFile = fileWrapper.getVirtualFile(true);
                if (savedFile != null) {
                    // 保存内容到文件
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        try {
                            savedFile.setBinaryContent(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            currentFile = savedFile;
                            isDocumentModified = false;
                            updateFilePathDisplay();
                            System.out.println("✅ 新建文档自动保存成功: " + savedFile.getName());
                            updateStatus("✅ 已保存: " + savedFile.getName());
                        } catch (Exception ex) {
                            System.err.println("❌ 保存文件失败: " + ex.getMessage());
                            updateStatus("❌ 保存失败: " + ex.getMessage());
                        }
                    });
                }
            } catch (Exception ex) {
                System.err.println("❌ 创建文件失败: " + ex.getMessage());
                updateStatus("❌ 保存失败: " + ex.getMessage());
            }
        }
    }
    
    public void dispose() {
        System.out.println("🗑️ 释放MarkdownToolWindow资源");
        
        // 🔍 检测是否为插件卸载操作
        boolean isPluginUninstalling = isPluginUninstallOperation();
        System.out.println("🔍 是否为插件卸载: " + isPluginUninstalling);
        
        try {
            if (isPluginUninstalling || isPluginUnloading) {
                // 插件卸载：阻止保存操作
                EMERGENCY_STOP_ALL_SAVES = true;
                isPluginUnloading = true;
                PLUGIN_IS_UNLOADING = true;
                System.out.println("🚨 检测到插件卸载，阻止保存操作");
            } else {
                // 正常的工具窗口关闭：触发自动保存
                System.out.println("ℹ️ 正常的工具窗口关闭，触发自动保存");
                autoSaveOnClose();
            }
            
            // 🚨 立即移除所有监听器，防止在dispose过程中触发事件
            if (document != null) {
                try {
                    // DocumentListener会在编辑器释放时自动移除，但为了保险起见手动清理
                    System.out.println("🔇 清理文档监听器");
                } catch (Exception ex) {
                    System.err.println("❌ 清理文档监听器时出错: " + ex.getMessage());
                }
            }
            
            // 🚨 移除层次监听器
            if (mainPanel != null) {
                try {
                    // 清除所有层次监听器
                    java.awt.event.HierarchyListener[] listeners = mainPanel.getHierarchyListeners();
                    for (java.awt.event.HierarchyListener listener : listeners) {
                        mainPanel.removeHierarchyListener(listener);
                    }
                    System.out.println("🔇 已移除 " + listeners.length + " 个层次监听器");
                } catch (Exception ex) {
                    System.err.println("❌ 移除层次监听器时出错: " + ex.getMessage());
                }
            }
    
            
            System.out.println("ℹ️ 插件正在卸载，强制跳过所有自动保存操作");
            
            // 释放编辑器资源
            if (editor != null && !editor.isDisposed()) {
                // 释放编辑器（会自动移除监听器）
                EditorFactory.getInstance().releaseEditor(editor);
                editor = null;
                System.out.println("🗑️ 已释放编辑器资源");
            }
            
            // 释放预览面板
            if (previewPanel != null) {
                previewPanel.dispose();
                previewPanel = null;
                System.out.println("🗑️ 已释放预览面板资源");
            }
            
            // 释放UI组件
            if (tabbedPane != null) {
                tabbedPane.removeAll();
                tabbedPane = null;
            }
            
            if (statusLabel != null) {
                statusLabel = null;
            }
            
            // 清空主面板 (但不能设为null，因为是final)
            if (mainPanel != null) {
                mainPanel.removeAll();
            }
            
            // 清空文档和文件引用
            document = null;
            currentFile = null;
            
            // 强制垃圾回收建议
            System.gc();
            
            System.out.println("✅ MarkdownToolWindow资源释放完成");
            
        } catch (Exception e) {
            System.err.println("❌ 释放MarkdownToolWindow资源时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
