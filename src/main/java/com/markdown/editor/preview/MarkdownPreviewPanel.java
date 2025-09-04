package com.markdown.editor.preview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefClient;
import com.intellij.ide.BrowserUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URL;

/**
 * Markdown预览面板
 * 使用JCEF浏览器组件来渲染HTML预览
 */
public class MarkdownPreviewPanel implements Disposable {
    private final Project project;
    private final JBCefBrowser browser;
    private final MarkdownProcessor processor;
    private final JPanel mainPanel;
    
    public MarkdownPreviewPanel(@NotNull Project project) {
        this.project = project;
        this.processor = new MarkdownProcessor();
        this.browser = createBrowser();
        this.mainPanel = createMainPanel();
        
        Disposer.register(this, browser);
    }
    
    private JBCefBrowser createBrowser() {
        JBCefBrowser browser = new JBCefBrowser();
        
        // 设置浏览器属性
        browser.getJBCefClient().addLoadHandler(new org.cef.handler.CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(org.cef.browser.CefBrowser browser, org.cef.browser.CefFrame frame, int httpStatusCode) {
                // 页面加载完成后的处理
                System.out.println("🔗 预览页面加载完成，已启用链接点击功能");
            }
        }, browser.getCefBrowser());
        
        // 🔗 添加链接点击监听器
        browser.getJBCefClient().addRequestHandler(new org.cef.handler.CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(org.cef.browser.CefBrowser browser, org.cef.browser.CefFrame frame, org.cef.network.CefRequest request, boolean userGesture, boolean isRedirect) {
                String url = request.getURL();
                System.out.println("🌐 检测到链接点击: " + url);
                
                // 检查是否应该拦截链接（排除一些特殊情况）
                if (url != null && shouldOpenInExternalBrowser(url)) {
                    System.out.println("🚀 在默认浏览器中打开链接: " + url);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            // 使用 IntelliJ 的 BrowserUtil 在默认浏览器中打开链接
                            BrowserUtil.browse(url);
                        } catch (Exception e) {
                            System.err.println("❌ 打开链接失败: " + e.getMessage());
                            // 作为备选方案，尝试使用系统默认方式
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(new URI(url));
                                }
                            } catch (Exception ex) {
                                System.err.println("❌ 备选方案也失败: " + ex.getMessage());
                            }
                        }
                    });
                    
                    // 返回 true 阻止在当前浏览器中导航
                    return true;
                }
                
                // 对于不需要外部打开的链接，允许正常导航
                return false;
            }
        }, browser.getCefBrowser());
        
        return browser;
    }
    
    /**
     * 判断链接是否应该在外部浏览器中打开
     * @param url 链接URL
     * @return true表示应该在外部浏览器打开，false表示在预览中正常导航
     */
    private boolean shouldOpenInExternalBrowser(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // 排除一些不应该在外部浏览器打开的URL
        String lowerUrl = url.toLowerCase().trim();
        
        // 排除JavaScript调用
        if (lowerUrl.startsWith("javascript:")) {
            System.out.println("🚫 跳过JavaScript链接: " + url);
            return false;
        }
        
        // 排除data URL
        if (lowerUrl.startsWith("data:")) {
            System.out.println("🚫 跳过data URL: " + url);
            return false;
        }
        
        // 排除about页面
        if (lowerUrl.startsWith("about:")) {
            System.out.println("🚫 跳过about页面: " + url);
            return false;
        }
        
        // 排除本地文件的特殊情况（如果有需要的话）
        if (lowerUrl.startsWith("file:") && lowerUrl.contains("temp")) {
            System.out.println("🚫 跳过临时文件: " + url);
            return false;
        }
        
        // 🌟 其他所有链接都在外部浏览器中打开
        // 包括：http://、https://、ftp://、mailto:、file://等
        System.out.println("✅ 允许在外部浏览器打开: " + url);
        return true;
    }
    
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 添加工具栏
        JToolBar toolBar = createToolBar();
        panel.add(toolBar, BorderLayout.NORTH);
        
        // 添加浏览器组件
        panel.add(browser.getComponent(), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 刷新按钮
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refresh());
        toolBar.add(refreshButton);
        
        // 分隔符
        toolBar.addSeparator();
        
        // 主题选择
        JComboBox<String> themeCombo = new JComboBox<>(new String[]{"GitHub", "暗黑", "简洁"});
        themeCombo.addActionListener(e -> {
            String selectedTheme = (String) themeCombo.getSelectedItem();
            processor.setTheme(selectedTheme);
            refresh();
        });
        toolBar.add(new JLabel("主题: "));
        toolBar.add(themeCombo);
        
        return toolBar;
    }
    
    /**
     * 更新预览内容
     * @param markdownContent Markdown文本内容
     */
    public void updateContent(String markdownContent) {
        // 保存当前内容，供刷新时使用
        this.currentMarkdownContent = markdownContent;
        
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String html = processor.processMarkdown(markdownContent);
                ApplicationManager.getApplication().invokeLater(() -> {
                    browser.loadHTML(html);
                });
            } catch (Exception e) {
                // 处理错误
                ApplicationManager.getApplication().invokeLater(() -> {
                    String errorHtml = createErrorHtml(e.getMessage());
                    browser.loadHTML(errorHtml);
                });
            }
        });
    }
    
    private String createErrorHtml(String errorMessage) {
        return String.format(
            "<html><head><title>预览错误</title></head>" +
            "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
            "<h2 style='color: #d73a49;'>预览错误</h2>" +
            "<p>渲染Markdown内容时发生错误：</p>" +
            "<pre style='background: #f6f8fa; padding: 10px; border-radius: 5px;'>%s</pre>" +
            "</body></html>",
            errorMessage
        );
    }
    
    /**
     * 刷新预览
     */
    public void refresh() {
        // 重新生成HTML内容以确保主题生效
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 获取当前文档内容
                String currentContent = getCurrentContent();
                if (currentContent != null) {
                    String html = processor.processMarkdown(currentContent);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        browser.loadHTML(html);
                    });
                } else {
                    // 如果没有内容，直接重新加载
                    browser.getCefBrowser().reload();
                }
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    browser.getCefBrowser().reload();
                });
            }
        });
    }
    
    private String currentMarkdownContent = "";
    
    /**
     * 获取当前内容
     */
    private String getCurrentContent() {
        return currentMarkdownContent;
    }
    
    /**
     * 设置预览主题
     */
    public void setTheme(String theme) {
        processor.setTheme(theme);
    }
    
    /**
     * 获取主面板组件
     */
    public JComponent getComponent() {
        return mainPanel;
    }
    
    /**
     * 滚动到指定位置
     */
    public void scrollToPosition(int line) {
        // 实现滚动到指定行的功能
        ApplicationManager.getApplication().invokeLater(() -> {
            String script = String.format("window.scrollTo(0, %d * 20);", line);
            browser.getCefBrowser().executeJavaScript(script, null, 0);
        });
    }

    @Override
    public void dispose() {
        // 清理资源
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
    }
}
