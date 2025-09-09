package com.markdown.editor.preview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ide.BrowserUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.net.URL;

/**
 * Markdown预览面板
 * 使用JTextPane实现，具备基础的HTML渲染能力和可靠的链接处理
 */
public class MarkdownPreviewPanel implements Disposable {
    private final Project project;
    private final JPanel mainPanel;
    private JTextPane textPane;
    private JScrollPane scrollPane;
    private String currentMarkdownContent = "";
    private final MarkdownProcessor processor;
    
    public MarkdownPreviewPanel(@NotNull Project project) {
        this.project = project;
        this.processor = new MarkdownProcessor();
        this.mainPanel = createMainPanel();
        
        System.out.println("✅ 预览面板初始化完成 (JTextPane简化模式)");
    }
    
    /**
     * 创建主面板
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(null);
        panel.setBackground(new java.awt.Color(43, 43, 43)); // #2B2B2B暗黑主题
        
        try {
            System.out.println("🔧 初始化JTextPane预览面板");
            
            // 创建JTextPane
            textPane = new JTextPane();
            textPane.setContentType("text/html");
            textPane.setEditable(false);
            textPane.setBorder(null);
            textPane.setMargin(new Insets(10, 10, 10, 10));
            
            // 设置暗黑主题背景
            textPane.setBackground(new java.awt.Color(43, 43, 43)); // #2B2B2B
            textPane.setOpaque(true);
            
            // 设置正常的选择颜色（恢复文本选择功能）
            textPane.setSelectionColor(new java.awt.Color(51, 153, 255)); // 正常的蓝色选择背景
            textPane.setSelectedTextColor(new java.awt.Color(255, 255, 255)); // 白色选择文本
            textPane.setCaretColor(new java.awt.Color(255, 255, 255)); // 白色光标
            
            // 设置HTML编辑器
            HTMLEditorKit kit = new HTMLEditorKit();
            textPane.setEditorKit(kit);
            
            // 设置样式表
            HTMLDocument doc = (HTMLDocument) textPane.getDocument();
            kit.getStyleSheet().addRule(getBasicCSS());
            
            // 设置链接处理
            setupLinkHandling();
            
            // 创建滚动面板
            scrollPane = new JScrollPane(textPane);
            scrollPane.setBorder(null);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            
            // 设置滚动面板暗黑主题
            scrollPane.setBackground(new java.awt.Color(43, 43, 43));
            scrollPane.setOpaque(true);
            scrollPane.getViewport().setBackground(new java.awt.Color(43, 43, 43));
            scrollPane.getViewport().setOpaque(true);
            
            panel.add(scrollPane, BorderLayout.CENTER);
            
            System.out.println("✅ JTextPane预览面板创建成功");
            
        } catch (Exception e) {
            System.err.println("❌ JTextPane预览面板创建失败: " + e.getMessage());
            e.printStackTrace();
            
            // 显示错误信息
            JLabel errorLabel = new JLabel("<html><center><h2>预览初始化失败</h2><p>" + e.getMessage() + "</p></center></html>");
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(errorLabel, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    /**
     * 获取JTextPane兼容的CSS样式
     */
    private String getBasicCSS() {
        return "body { font-family: Arial, sans-serif; font-size: 11px; color: #E6E6E6; background-color: #2B2B2B; margin: 0; padding: 8px; }" +
               "h1 { font-size: 18px; font-weight: bold; color: #FFFFFF; margin-top: 14px; margin-bottom: 6px; }" +
               "h2 { font-size: 16px; font-weight: bold; color: #FFFFFF; margin-top: 12px; margin-bottom: 5px; }" +
               "h3 { font-size: 14px; font-weight: bold; color: #FFFFFF; margin-top: 10px; margin-bottom: 4px; }" +
               "h4 { font-size: 13px; font-weight: bold; color: #CCCCCC; margin-top: 8px; margin-bottom: 3px; }" +
               "h5 { font-size: 12px; font-weight: bold; color: #CCCCCC; margin-top: 6px; margin-bottom: 2px; }" +
               "h6 { font-size: 11px; font-weight: bold; color: #CCCCCC; margin-top: 6px; margin-bottom: 2px; }" +
               "p { font-size: 11px; color: #E6E6E6; margin-top: 3px; margin-bottom: 6px; }" +
               "pre { color: #D4D4D4; font-family: monospace; font-size: 10px; padding: 8px 0; margin: 0; border: none; background: transparent; }" +
               "code { color: #E6E6E6; font-family: monospace; font-size: 10px; padding: 0; background: transparent; }" +
               "blockquote { color: #999999; font-style: italic; border-left: 3px solid #555555; padding-left: 10px; margin: 6px 0; }" +
               "a { color: #4FC3F7; text-decoration: underline; }" +
               "strong { font-weight: bold; color: #FFFFFF; }" +
               "em { font-style: italic; color: #E6E6E6; }" +
               "ul { margin: 4px 0; padding-left: 20px; color: #E6E6E6; list-style-type: disc; }" +
               "ol { margin: 4px 0; padding-left: 20px; color: #E6E6E6; list-style-type: decimal; }" +
               "li { margin: 2px 0; color: #E6E6E6; display: list-item; }" +
               "table { border: 1px solid #555555; margin: 6px 0; border-collapse: collapse; }" +
               "th { font-weight: bold; background-color: #404040; color: #FFFFFF; padding: 6px; border: 1px solid #555555; font-size: 11px; }" +
               "td { color: #E6E6E6; padding: 6px; border: 1px solid #555555; font-size: 11px; }" +
               "hr { border: none; border-top: 1px solid #555555; margin: 10px 0; }";
    }
    
    /**
     * 设置链接处理
     */
    private void setupLinkHandling() {
        if (textPane == null) return;
        
        try {
            System.out.println("🔗 设置JTextPane链接处理");
            
            textPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            String url = null;
                            
                            // 获取链接URL
                            if (e.getURL() != null) {
                                url = e.getURL().toString();
                            } else if (e.getDescription() != null) {
                                url = e.getDescription();
                            }
                            
                            if (url != null && !url.trim().isEmpty()) {
                                System.out.println("🔗 捕获链接点击: " + url);
                                
                                                // 检查是否是代码块折叠链接
                if (url.startsWith("fold://")) {
                    System.out.println("🔗 检测到代码块折叠链接: " + url);
                    handleCodeBlockFold(url);
                    return; // 重要：避免继续处理
                } else {
                    System.out.println("🔗 检测到普通链接: " + url);
                    openLinkInBrowser(url);
                }
                            }
                            
                        } catch (Exception ex) {
                            System.err.println("❌ 链接处理失败: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                }
            });
            
            System.out.println("✅ JTextPane链接处理设置完成");
            
        } catch (Exception e) {
            System.err.println("❌ JTextPane链接处理设置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理代码块折叠切换
     */
    private void handleCodeBlockFold(String foldUrl) {
        try {
            // 提取代码块ID
            String codeBlockId = foldUrl.substring("fold://".length());
            System.out.println("🔀 处理代码块折叠: " + codeBlockId);
            
            // 切换折叠状态
            boolean newState = processor.toggleCodeBlockFold(codeBlockId);
            System.out.println("🔀 代码块 " + codeBlockId + " 新状态: " + (newState ? "折叠" : "展开"));
            
            // 重新渲染内容以更新折叠状态
            if (currentMarkdownContent != null) {
                System.out.println("🔄 重新渲染内容以更新折叠状态");
                
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        if (textPane != null) {
                            // 清空当前内容
                            textPane.setText("");
                            
                            // 重新加载内容
                            loadContentInTextPane(currentMarkdownContent);
                            
                            System.out.println("✅ 内容重新渲染完成");
                        }
                    } catch (Exception ex) {
                        System.err.println("❌ 重新渲染失败: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            }
            
        } catch (Exception e) {
            System.err.println("❌ 代码块折叠处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 在外部浏览器中打开链接
     */
    private void openLinkInBrowser(String url) {
        try {
            System.out.println("🌐 准备在外部浏览器中打开: " + url);
            
            // 处理URL格式
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("mailto:") && !url.startsWith("file:")) {
                if (url.contains("@") && !url.contains(".") || url.matches(".*@.*\\..+")) {
                    url = "mailto:" + url;
                    System.out.println("🔗 识别为邮箱链接: " + url);
                } else {
                    url = "https://" + url;
                    System.out.println("🔗 添加https前缀: " + url);
                }
            }
            
            // 使用BrowserUtil打开链接
            BrowserUtil.browse(url);
            
            System.out.println("✅ 链接已在外部浏览器中打开: " + url);
            
        } catch (Exception e) {
            System.err.println("❌ 打开链接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 更新预览内容 (优化内存管理版本)
     */
    public void updateContent(@NotNull String markdownContent) {
        // 防御性检查
        if (textPane == null || processor == null) {
            System.err.println("⚠️ 组件未就绪，无法更新内容");
            return;
        }
        
        // 内容相同时不更新，节省资源
        if (markdownContent.equals(currentMarkdownContent)) {
            return;
        }
        
        // 防止内容过大导致内存问题
        if (markdownContent.length() > 1_000_000) { // 1MB限制
            System.err.println("⚠️ Markdown内容过大，可能导致性能问题");
            currentMarkdownContent = markdownContent.substring(0, 1_000_000) + "\n\n... (内容已截断以防止性能问题)";
        } else {
            currentMarkdownContent = markdownContent;
        }
        
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (textPane != null && !textPane.getText().isEmpty()) {
                    // 清空之前的内容以释放内存
                    textPane.setText("");
                }
                
                if (textPane != null) {
                    loadContentInTextPane(currentMarkdownContent);
                }
            } catch (Exception e) {
                System.err.println("❌ 更新预览内容失败: " + e.getMessage());
                e.printStackTrace();
                
                // 发生错误时显示简单错误信息，避免保留大量内容
                if (textPane != null) {
                    textPane.setText("<html><body><h3>预览错误</h3><p>" + e.getMessage() + "</p></body></html>");
                }
            }
        });
    }
    
    /**
     * 在JTextPane中加载内容
     */
    private void loadContentInTextPane(String markdownContent) {
        try {
            System.out.println("📝 在JTextPane中加载Markdown内容");
            
            // 调试：输出生成的HTML
            if (markdownContent.contains("```")) {
                System.out.println("🔍 检测到代码块，启用调试模式");
                processor.debugGeneratedHtml(markdownContent);
            }
            
            // 使用MarkdownProcessor处理内容
            String html = processor.processMarkdown(markdownContent);
            
            // 额外调试：检查HTML中是否包含折叠结构
            if (html.contains("fold://")) {
                System.out.println("✅ HTML包含折叠链接");
                System.out.println("🔍 折叠链接数量: " + (html.split("fold://").length - 1));
            } else {
                System.out.println("❌ HTML不包含折叠链接");
            }
            
            // 确保HTML有基本结构
            if (!html.contains("<html>")) {
                html = "<html><body>" + html + "</body></html>";
            }
            
            // 设置HTML内容
            textPane.setText(html);
            
            // 滚动到顶部
            SwingUtilities.invokeLater(() -> {
                textPane.setCaretPosition(0);
            });
            
            System.out.println("✅ JTextPane内容加载完成");
            
        } catch (Exception e) {
            System.err.println("❌ JTextPane内容加载失败: " + e.getMessage());
            e.printStackTrace();
            
            // 显示错误内容
            textPane.setText("<html><body><h2>预览加载失败</h2><p>" + e.getMessage() + "</p></body></html>");
        }
    }
    
    /**
     * 刷新预览 - 强制重新渲染，不依赖缓存
     */
    public void refresh() {
        System.out.println("🔄 刷新JTextPane预览");
        
        String content = currentMarkdownContent;
        currentMarkdownContent = ""; // 清除缓存
        updateContent(content); // 强制重新加载
    }
    
    /**
     * 获取主面板组件
     */
    public JComponent getComponent() {
        return mainPanel;
    }
    
    
    @Override
    public void dispose() {
        System.out.println("🗑️ 释放JTextPane预览面板资源");
        
        try {
            // 清空当前内容缓存
            currentMarkdownContent = null;
            
            // 释放JTextPane资源
            if (textPane != null) {
                // 移除所有超链接监听器
                HyperlinkListener[] listeners = textPane.getHyperlinkListeners();
                for (HyperlinkListener listener : listeners) {
                    textPane.removeHyperlinkListener(listener);
                    System.out.println("🗑️ 已移除HyperlinkListener");
                }
                
                // 清空编辑器
                textPane.setDocument(new javax.swing.text.DefaultStyledDocument());
                textPane = null;
            }
            
            // 释放滚动面板
            if (scrollPane != null) {
                scrollPane.removeAll();
                scrollPane.setViewport(null);
                scrollPane = null;
            }
            
            // 清空主面板 (但不能设为null，因为是final)
            if (mainPanel != null) {
                mainPanel.removeAll();
            }
            
            // 释放处理器资源 (但不能设为null，因为是final)
            if (processor != null) {
                processor.dispose();
            }
            
            // 强制垃圾回收建议
            System.gc();
            
            System.out.println("✅ JTextPane预览面板资源释放完成");
            
        } catch (Exception e) {
            System.err.println("❌ 释放预览面板资源时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}