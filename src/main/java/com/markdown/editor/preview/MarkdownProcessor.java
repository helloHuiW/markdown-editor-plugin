package com.markdown.editor.preview;

import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.MarkdownParser;

/**
 * Markdown处理器
 * 负责将Markdown文本转换为HTML
 */
public class MarkdownProcessor {
    private String currentTheme = "GitHub";
    private final MarkdownParser parser;
    private final GFMFlavourDescriptor flavour;
    
    public MarkdownProcessor() {
        this.flavour = new GFMFlavourDescriptor();
        this.parser = new MarkdownParser(flavour);
    }
    
    /**
     * 处理Markdown文本并转换为HTML
     * @param markdownText 原始Markdown文本
     * @return 渲染后的HTML字符串
     */
    public String processMarkdown(String markdownText) {
        if (markdownText == null || markdownText.trim().isEmpty()) {
            return createEmptyHtml();
        }
        
        try {
            // 使用简化的HTML生成，直接将Markdown文本转换为基本HTML
            String basicHtml = convertToBasicHtml(markdownText);
            return wrapHtmlDocument(basicHtml);
            
        } catch (Exception e) {
            return createErrorHtml("解析错误: " + e.getMessage());
        }
    }
    
    /**
     * 设置预览主题
     */
    public void setTheme(String theme) {
        this.currentTheme = theme;
    }
    
    /**
     * 包装HTML文档
     */
    private String wrapHtmlDocument(String bodyContent) {
        String css = getCssForTheme(currentTheme);
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>Markdown预览</title>" +
            "<style>%s</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"markdown-body\">" +
            "%s" +
            "</div>" +
            "<script>%s</script>" +
            "</body>" +
            "</html>",
            css, bodyContent, getJavaScript()
        );
    }
    
    /**
     * 获取主题CSS
     */
    private String getCssForTheme(String theme) {
        switch (theme) {
            case "暗黑":
                return getDarkThemeCss();
            case "简洁":
                return getMinimalThemeCss();
            case "GitHub":
            default:
                return getGitHubThemeCss();
        }
    }
    
    private String getGitHubThemeCss() {
        return 
            ".markdown-body {" +
            "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;" +
            "  font-size: 16px;" +
            "  line-height: 1.6;" +
            "  color: #24292f;" +
            "  background-color: #ffffff;" +
            "  padding: 20px;" +
            "  max-width: 1000px;" +
          
            "  margin: 0 auto;" +
            "}" +
            ".markdown-body p {" +
            "  margin: 0 0 16px 0;" +
            "  line-height: 1.6;" +
            "}" +
            ".markdown-body h1, .markdown-body h2 {" +
            "  border-bottom: 1px solid #d0d7de;" +
            "  padding-bottom: 0.3em;" +
            "  margin-top: 24px;" +
            "  margin-bottom: 16px;" +
            "}" +
            ".markdown-body h1 { font-size: 2em; }" +
            ".markdown-body h2 { font-size: 1.5em; }" +
            ".markdown-body h3 { font-size: 1.25em; margin: 20px 0 16px 0; }" +
            ".markdown-body h4, .markdown-body h5, .markdown-body h6 { margin: 16px 0 12px 0; }" +
            ".markdown-body code {" +
            "  background-color: rgba(175, 184, 193, 0.2);" +
            "  padding: 0.2em 0.4em;" +
            "  border-radius: 3px;" +
            "  font-family: 'Courier New', Consolas, 'Liberation Mono', Menlo, monospace;" +
            "  font-size: 85%;" +
            "  border: 1px solid #e1e4e8;" +
            "}" +
            "/* GitHub主题 - 代码块样式 */" +
            ".code-block-container {" +
            "  margin: 16px 0;" +
            "  border-radius: 6px;" +
            "  background-color: #f6f8fa;" +
            "  border: 1px solid #e1e4e8;" +
            "  padding: 16px;" +
            "  overflow-x: auto;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;" +
            "  font-size: 14px;" +
            "  line-height: 1.4;" +
            "}" +
            ".code-line {" +
            "  margin: 0;" +
            "  padding: 0;" +
            "  color: #24292f;" +
            "  white-space: pre;" +
            "}" +
            ".code-block-pre {" +
            "  margin: 0 !important;" +
            "  padding: 16px !important;" +
            "  background-color: transparent !important;" +
            "  border: none !important;" +
            "  white-space: pre !important;" +
            "  overflow-x: auto;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;" +
            "  font-size: 14px !important;" +
            "  line-height: 1.4 !important;" +
            "  tab-size: 4;" +
            "}" +
            ".code-block-code {" +
            "  font-family: inherit !important;" +
            "  font-size: inherit !important;" +
            "  color: #24292f !important;" +
            "  background-color: transparent !important;" +
            "  padding: 0 !important;" +
            "  margin: 0 !important;" +
            "  white-space: pre !important;" +
            "  display: block !important;" +
            "  line-height: inherit !important;" +
            "  word-wrap: normal !important;" +
            "  word-break: normal !important;" +
            "  overflow-wrap: normal !important;" +
            "}" +
            "/* 兼容性：保留原有样式 */" +
            ".markdown-body pre {" +
            "  background-color: #f6f8fa;" +
            "  border: 1px solid #e1e4e8;" +
            "  border-radius: 6px;" +
            "  padding: 16px;" +
            "  overflow-x: auto;" +
            "  margin: 0 0 16px 0;" +
            "  line-height: 1.45;" +
            "  white-space: pre !important;" +
            "  tab-size: 4;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;" +
            "  font-size: 14px;" +
            "}" +
            ".markdown-body pre code {" +
            "  background-color: transparent !important;" +
            "  border: none !important;" +
            "  padding: 0 !important;" +
            "  margin: 0 !important;" +
            "  font-size: 14px !important;" +
            "  color: #24292f;" +
            "  white-space: pre !important;" +
            "  word-wrap: normal !important;" +
            "  tab-size: 4 !important;" +
            "  font-family: inherit !important;" +
            "  display: block !important;" +
            "  line-height: 1.45 !important;" +
            "}" +
            ".markdown-body blockquote {" +
            "  border-left: 0.25em solid #d0d7de;" +
            "  padding-left: 1em;" +
            "  color: #656d76;" +
            "}" +
            ".markdown-body ul, .markdown-body ol {" +
            "  padding-left: 2em;" +
            "  margin: 0 0 16px 0;" +
            "}" +
            ".markdown-body li {" +
            "  margin: 0.25em 0;" +
            "  line-height: 1.5;" +
            "}" +
            ".markdown-body ul {" +
            "  list-style-type: disc;" +
            "}" +
            ".markdown-body ol {" +
            "  list-style-type: decimal;" +
            "}" +
            ".markdown-body table {" +
            "  border-collapse: collapse;" +
            "  width: 100%;" +
            "}" +
            ".markdown-body th, .markdown-body td {" +
            "  border: 1px solid #d0d7de;" +
            "  padding: 8px 12px;" +
            "  text-align: left;" +
            "}" +
            ".markdown-body th {" +
            "  background-color: #f6f8fa;" +
            "  font-weight: 600;" +
            "}" +
            ".markdown-body table {" +
            "  border-collapse: collapse;" +
            "  margin: 0 0 16px 0;" +
            "  width: 100%;" +
            "  overflow: auto;" +
            "}";
    }
    
    private String getDarkThemeCss() {
        return 
            ".markdown-body {" +
            "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;" +
            "  font-size: 16px;" +
            "  line-height: 1.6;" +
            "  color: #c9d1d9;" +
            "  background-color: #0d1117;" +
            "  padding: 20px;" +
            "  max-width: 1000px;" +
            "  margin: 0 auto;" +
            "}" +
            ".markdown-body p {" +
            "  margin: 0 0 16px 0;" +
            "  line-height: 1.6;" +
            "}" +
            ".markdown-body h1, .markdown-body h2 {" +
            "  border-bottom: 1px solid #30363d;" +
            "  padding-bottom: 0.3em;" +
            "  color: #f0f6fc;" +
            "  margin-top: 24px;" +
            "  margin-bottom: 16px;" +
            "}" +
            ".markdown-body code {" +
            "  background-color: rgba(110, 118, 129, 0.4);" +
            "  color: #e6edf3;" +
            "  padding: 0.2em 0.4em;" +
            "  border-radius: 3px;" +
            "  font-family: 'Courier New', Consolas, 'Liberation Mono', Menlo, monospace;" +
            "  font-size: 85%;" +
            "  border: 1px solid #30363d;" +
            "}" +
            "/* 暗黑主题 - 代码块样式 */" +
            ".code-block-container {" +
            "  margin: 16px 0;" +
            "  border-radius: 6px;" +
            "  background-color: #161b22;" +
            "  border: 1px solid #30363d;" +
            "  padding: 16px;" +
            "  overflow-x: auto;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;" +
            "  font-size: 14px;" +
            "  line-height: 1.4;" +
            "}" +
            ".code-line {" +
            "  margin: 0;" +
            "  padding: 0;" +
            "  color: #e6edf3;" +
            "  white-space: pre;" +
            "}" +
            ".code-block-pre {" +
            "  margin: 0 !important;" +
            "  padding: 16px !important;" +
            "  background-color: transparent !important;" +
            "  border: none !important;" +
            "  white-space: pre !important;" +
            "  overflow-x: auto;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;" +
            "  font-size: 14px !important;" +
            "  line-height: 1.4 !important;" +
            "  tab-size: 4;" +
            "}" +
            ".code-block-code {" +
            "  font-family: inherit !important;" +
            "  font-size: inherit !important;" +
            "  color: #e6edf3 !important;" +
            "  background-color: transparent !important;" +
            "  padding: 0 !important;" +
            "  margin: 0 !important;" +
            "  white-space: pre !important;" +
            "  display: block !important;" +
            "  line-height: inherit !important;" +
            "  word-wrap: normal !important;" +
            "  word-break: normal !important;" +
            "  overflow-wrap: normal !important;" +
            "}" +
            "/* 兼容性：保留原有样式 */" +
            ".markdown-body pre {" +
            "  background-color: #161b22;" +
            "  border: 1px solid #30363d;" +
            "  border-radius: 6px;" +
            "  padding: 16px;" +
            "  overflow-x: auto;" +
            "  margin: 0 0 16px 0;" +
            "  line-height: 1.45;" +
            "  white-space: pre !important;" +
            "  tab-size: 4;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;" +
            "  font-size: 14px;" +
            "}" +
            ".markdown-body pre code {" +
            "  background-color: transparent !important;" +
            "  border: none !important;" +
            "  padding: 0 !important;" +
            "  margin: 0 !important;" +
            "  font-size: 14px !important;" +
            "  color: #e6edf3;" +
            "  white-space: pre !important;" +
            "  word-wrap: normal !important;" +
            "  tab-size: 4 !important;" +
            "  font-family: inherit !important;" +
            "  display: block !important;" +
            "  line-height: 1.45 !important;" +
            "}" +
            ".markdown-body blockquote {" +
            "  border-left: 0.25em solid #30363d;" +
            "  padding-left: 1em;" +
            "  color: #8b949e;" +
            "}" +
            ".markdown-body ul, .markdown-body ol {" +
            "  padding-left: 2em;" +
            "  margin: 0 0 16px 0;" +
            "}" +
            ".markdown-body li {" +
            "  margin: 0.25em 0;" +
            "  line-height: 1.5;" +
            "  color: #c9d1d9;" +
            "}" +
            ".markdown-body ul {" +
            "  list-style-type: disc;" +
            "}" +
            ".markdown-body ol {" +
            "  list-style-type: decimal;" +
            "}" +
            ".markdown-body table {" +
            "  border-collapse: collapse;" +
            "  width: 100%;" +
            "}" +
            ".markdown-body th, .markdown-body td {" +
            "  border: 1px solid #30363d;" +
            "  padding: 8px 12px;" +
            "  text-align: left;" +
            "}" +
            ".markdown-body th {" +
            "  background-color: #161b22;" +
            "  font-weight: 600;" +
            "  color: #f0f6fc;" +
            "}" +
            ".markdown-body table {" +
            "  border-collapse: collapse;" +
            "  margin: 0 0 16px 0;" +
            "  width: 100%;" +
            "  overflow: auto;" +
            "}";
    }
    
    private String getMinimalThemeCss() {
        return 
            ".markdown-body {" +
            "  font-family: Georgia, 'Times New Roman', serif;" +
            "  font-size: 18px;" +
            "  line-height: 1.7;" +
            "  color: #333;" +
            "  background-color: #fff;" +
            "  padding: 40px;" +
            "  max-width: 800px;" +
            "  margin: 0 auto;" +
            "}" +
            ".markdown-body p {" +
            "  margin: 0 0 18px 0;" +
            "  line-height: 1.7;" +
            "}" +
            ".markdown-body h1, .markdown-body h2, .markdown-body h3 {" +
            "  font-family: 'Helvetica Neue', Arial, sans-serif;" +
            "  margin-top: 32px;" +
            "  margin-bottom: 18px;" +
            "}" +
            ".markdown-body h1 { font-size: 2.2em; }" +
            ".markdown-body h2 { font-size: 1.8em; }" +
            ".markdown-body h3 { font-size: 1.4em; }" +
            ".markdown-body code {" +
            "  background-color: #f5f5f5;" +
            "  padding: 0.2em 0.4em;" +
            "  border-radius: 3px;" +
            "  font-family: 'Courier New', Consolas, 'Liberation Mono', Menlo, monospace;" +
            "  font-size: 85%;" +
            "  border: 1px solid #e0e0e0;" +
            "}" +
            "/* 简洁主题 - 代码块样式 */" +
            ".code-block-container {" +
            "  margin: 18px 0;" +
            "  border-radius: 6px;" +
            "  background-color: #f8f8f8;" +
            "  border: 1px solid #e0e0e0;" +
            "  padding: 20px;" +
            "  overflow-x: auto;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;" +
            "  font-size: 14px;" +
            "  line-height: 1.5;" +
            "}" +
            ".code-line {" +
            "  margin: 0;" +
            "  padding: 0;" +
            "  color: #333;" +
            "  white-space: pre;" +
            "}" +
            ".code-block-pre {" +
            "  margin: 0 !important;" +
            "  padding: 20px !important;" +
            "  background-color: transparent !important;" +
            "  border: none !important;" +
            "  white-space: pre !important;" +
            "  overflow-x: auto;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;" +
            "  font-size: 14px !important;" +
            "  line-height: 1.4 !important;" +
            "  tab-size: 4;" +
            "}" +
            ".code-block-code {" +
            "  font-family: inherit !important;" +
            "  font-size: inherit !important;" +
            "  color: #333 !important;" +
            "  background-color: transparent !important;" +
            "  padding: 0 !important;" +
            "  margin: 0 !important;" +
            "  white-space: pre !important;" +
            "  display: block !important;" +
            "  line-height: inherit !important;" +
            "  word-wrap: normal !important;" +
            "  word-break: normal !important;" +
            "  overflow-wrap: normal !important;" +
            "}" +
            "/* 兼容性：保留原有样式 */" +
            ".markdown-body pre {" +
            "  background-color: #f8f8f8;" +
            "  border: 1px solid #e0e0e0;" +
            "  border-radius: 6px;" +
            "  padding: 20px;" +
            "  overflow-x: auto;" +
            "  margin: 0 0 18px 0;" +
            "  line-height: 1.5;" +
            "  white-space: pre !important;" +
            "  tab-size: 4;" +
            "  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;" +
            "  font-size: 14px;" +
            "}" +
            ".markdown-body pre code {" +
            "  background-color: transparent !important;" +
            "  border: none !important;" +
            "  padding: 0 !important;" +
            "  margin: 0 !important;" +
            "  font-size: 14px !important;" +
            "  color: #333;" +
            "  white-space: pre !important;" +
            "  word-wrap: normal !important;" +
            "  tab-size: 4 !important;" +
            "  font-family: inherit !important;" +
            "  display: block !important;" +
            "  line-height: 1.5 !important;" +
            "}" +
            ".markdown-body ul, .markdown-body ol {" +
            "  padding-left: 2em;" +
            "  margin: 0 0 18px 0;" +
            "}" +
            ".markdown-body li {" +
            "  margin: 0.3em 0;" +
            "  line-height: 1.6;" +
            "  color: #333;" +
            "}" +
            ".markdown-body ul {" +
            "  list-style-type: disc;" +
            "}" +
            ".markdown-body ol {" +
            "  list-style-type: decimal;" +
            "}" +
            ".markdown-body del {" +
            "  text-decoration: line-through;" +
            "  color: #666;" +
            "}" +
            ".markdown-body hr {" +
            "  border: none;" +
            "  border-top: 1px solid #e0e0e0;" +
            "  margin: 24px 0;" +
            "  height: 0;" +
            "}";
    }
    
    private String getJavaScript() {
        return 
            "// 平滑滚动" +
            "function scrollToLine(line) {" +
            "  var target = line * 24;" +
            "  window.scrollTo({top: target, behavior: 'smooth'});" +
            "}" +
            "// 代码块高亮支持" +
            "document.addEventListener('DOMContentLoaded', function() {" +
            "  var codeBlocks = document.querySelectorAll('pre code');" +
            "  for (var i = 0; i < codeBlocks.length; i++) {" +
            "    codeBlocks[i].classList.add('language-' + (codeBlocks[i].className || 'text'));" +
            "  }" +
            "});";
    }
    
    private String createEmptyHtml() {
        return wrapHtmlDocument("<p style='color: #888; text-align: center; margin-top: 50px;'>开始编写Markdown内容...</p>");
    }
    
    private String createErrorHtml(String error) {
        return wrapHtmlDocument(
            "<div style='color: #d73a49; background: #ffeef0; padding: 20px; border-radius: 6px; margin: 20px 0;'>" +
            "<h3>渲染错误</h3>" +
            "<p>" + error + "</p>" +
            "</div>"
        );
    }
    
    /**
     * 简化的Markdown到HTML转换
     */
    private String convertToBasicHtml(String markdownText) {
        if (markdownText == null || markdownText.trim().isEmpty()) {
            return "<p style='color: #888; text-align: center; margin-top: 50px;'>开始编写Markdown内容...</p>";
        }
        
        String html = markdownText;
        
        // 处理标题
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^##### (.+)$", "<h5>$1</h5>");
        html = html.replaceAll("(?m)^###### (.+)$", "<h6>$1</h6>");
        
        // 处理粗体和斜体
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        
        // 先处理代码块（必须在行内代码之前处理，避免冲突）
        // 重要：保持代码块内的所有空格、制表符和换行
        html = processCodeBlocksSimple(html);
        
        // 处理行内代码（在代码块之后处理）
        html = html.replaceAll("`([^`]+?)`", "<code>$1</code>");
        
        // 处理链接
        html = html.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href=\"$2\">$1</a>");
        
        // 处理图片
        html = html.replaceAll("!\\[(.+?)\\]\\((.+?)\\)", "<img src=\"$2\" alt=\"$1\" />");
        
        // 处理引用
        html = html.replaceAll("(?m)^> (.+)$", "<blockquote>$1</blockquote>");
        
        // 处理有序列表
        html = html.replaceAll("(?m)^\\d+\\. (.+)$", "<li>$1</li>");
        
        // 处理无序列表
        html = html.replaceAll("(?m)^[-*+] (.+)$", "<li>$1</li>");
        
        // 处理水平分割线
        html = html.replaceAll("(?m)^---+$", "<hr>");
        html = html.replaceAll("(?m)^\\*\\*\\*+$", "<hr>");
        html = html.replaceAll("(?m)^___+$", "<hr>");
        
        // 处理表格
        html = processSimpleTables(html);
        
        // 处理强制换行（两个空格+换行）
        html = html.replaceAll("  \\n", "<br>\n");
        
        // 首先移除多余的空行
        html = html.replaceAll("\\n\\s*\\n", "\n\n");
        
        // 分行处理，组装最终HTML
        return processLinesImproved(html);
    }
    
    private String processSimpleTables(String html) {
        String[] lines = html.split("\\n");
        StringBuilder result = new StringBuilder();
        boolean inTable = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 检查是否是表格行（包含 |）
            if (line.contains("|") && !line.isEmpty()) {
                // 检查下一行是否是分隔符（包含 --- 或 :---: 等）
                if (i + 1 < lines.length && lines[i + 1].trim().matches(".*[-:]+.*")) {
                    if (!inTable) {
                        result.append("<table>\n<thead>\n");
                        inTable = true;
                    }
                    // 处理表头
                    result.append("<tr>");
                    String[] headers = line.split("\\|");
                    for (String header : headers) {
                        String trimmed = header.trim();
                        if (!trimmed.isEmpty()) {
                            result.append("<th>").append(trimmed).append("</th>");
                        }
                    }
                    result.append("</tr>\n</thead>\n<tbody>\n");
                    i++; // 跳过分隔符行
                } else if (inTable) {
                    // 处理表格数据行
                    result.append("<tr>");
                    String[] cells = line.split("\\|");
                    for (String cell : cells) {
                        String trimmed = cell.trim();
                        if (!trimmed.isEmpty()) {
                            result.append("<td>").append(trimmed).append("</td>");
                        }
                    }
                    result.append("</tr>\n");
                } else {
                    result.append(line).append("\n");
                }
            } else {
                if (inTable) {
                    result.append("</tbody>\n</table>\n");
                    inTable = false;
                }
                result.append(line).append("\n");
            }
        }
        
        if (inTable) {
            result.append("</tbody>\n</table>\n");
        }
        
        return result.toString();
    }
    
    private String processCodeBlocksSimple(String html) {
        System.out.println("🔄 使用全新的简单代码块处理器");
        
        // 使用最简单的字符串替换方法，避免复杂的正则表达式和状态机
        StringBuilder result = new StringBuilder();
        int index = 0;
        
        while (index < html.length()) {
            int codeStart = html.indexOf("```", index);
            if (codeStart == -1) {
                // 没有更多代码块
                result.append(html.substring(index));
                break;
            }
            
            // 添加代码块前的内容
            result.append(html.substring(index, codeStart));
            
            // 查找代码块结束
            int lineEnd = html.indexOf('\n', codeStart);
            if (lineEnd == -1) {
                // 没有换行，不是有效的代码块
                result.append(html.substring(codeStart));
                break;
            }
            
            // 提取语言标识
            String language = html.substring(codeStart + 3, lineEnd).trim();
            System.out.println("🎯 发现代码块，语言: '" + language + "'");
            
            // 查找代码块结束标记
            int codeEnd = html.indexOf("\n```", lineEnd);
            if (codeEnd == -1) {
                // 没有结束标记，处理到最后
                String code = html.substring(lineEnd + 1);
                result.append(createCodeBlockHtml(language, code));
                break;
            }
            
            // 提取代码内容（保持原始格式）
            String code = html.substring(lineEnd + 1, codeEnd);
            System.out.println("📝 提取的代码:");
            System.out.println("   长度: " + code.length() + " 字符");
            System.out.println("   行数: " + code.split("\n").length);
            System.out.println("   内容预览: '" + code.substring(0, Math.min(100, code.length())).replace("\n", "\\n").replace("\t", "\\t") + "'");
            
            // 生成HTML（使用特殊的保格式方法）
            result.append(createCodeBlockHtml(language, code));
            
            // 继续处理后续内容
            index = codeEnd + 4; // 跳过 "\n```"
        }
        
        return result.toString();
    }
    
    private String processCodeBlocks(String html) {
        StringBuilder result = new StringBuilder();
        
        // 使用正则表达式匹配代码块，保持完整的内容结构
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("```([a-zA-Z0-9+#-]*)\\n(.*?)\\n```", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(html);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // 添加代码块前的内容
            result.append(html.substring(lastEnd, matcher.start()));
            
            // 处理代码块
            String language = matcher.group(1) != null ? matcher.group(1).trim() : "";
            String code = matcher.group(2) != null ? matcher.group(2) : "";
            
            // 关键：完全保持代码的原始格式，包括前导空格
            result.append("<pre><code class=\"language-").append(language).append("\">")
                  .append(escapeHtmlPreserveSpaces(code))
                  .append("</code></pre>");
                  
            lastEnd = matcher.end();
        }
        
        // 添加剩余内容
        result.append(html.substring(lastEnd));
        
        return result.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private String createCodeBlockHtml(String language, String code) {
        System.out.println("🔨 生成代码块HTML:");
        System.out.println("   语言: '" + language + "'");
        System.out.println("   原始代码长度: " + code.length());
        System.out.println("   原始代码行数: " + (code.split("\n").length));
        
        // 🚨 使用最原始但最可靠的方法：手动处理每一行
        String[] lines = code.split("\n");
        StringBuilder htmlContent = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // HTML转义但保持空格
            String escapedLine = escapeHtmlButKeepSpaces(line);
            
            htmlContent.append("<div class=\"code-line\">");
            htmlContent.append(escapedLine);
            htmlContent.append("</div>");
            
            if (i < lines.length - 1) {
                // 不是最后一行，添加换行标记
                System.out.println("   行 " + (i+1) + ": '" + escapedLine + "'");
            }
        }
        
        // 生成基于div行结构的HTML（移除硬编码的背景色，使用CSS主题）
        String html = "<div class=\"code-block-container\">" +
                     htmlContent.toString() +
                     "</div>";
        
        System.out.println("🏗️ 生成的HTML长度: " + html.length());
        System.out.println("🏗️ 总行数: " + lines.length);
        System.out.println("🏗️ HTML预览: " + html.substring(0, Math.min(300, html.length())));
        
        return html;
    }
    
    private String escapeHtmlButKeepSpaces(String line) {
        if (line == null) return "";
        
        // 分析这一行
        int spaceCount = 0;
        int tabCount = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') spaceCount++;
            else if (c == '\t') tabCount++;
        }
        
        // HTML转义，但用&nbsp;替换空格以确保保持
        String result = line;
        result = result.replace("&", "&amp;");  // 必须第一个处理
        result = result.replace("<", "&lt;");
        result = result.replace(">", "&gt;");
        result = result.replace("\"", "&quot;");
        result = result.replace("'", "&#39;");
        
        // 🔑 关键：将前导空格替换为&nbsp;以确保缩进保持
        StringBuilder processedLine = new StringBuilder();
        boolean foundNonSpace = false;
        for (char c : result.toCharArray()) {
            if (c == ' ' && !foundNonSpace) {
                // 前导空格用&nbsp;替换
                processedLine.append("&nbsp;");
            } else if (c == '\t' && !foundNonSpace) {
                // 前导制表符用4个&nbsp;替换
                processedLine.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            } else {
                foundNonSpace = true;
                if (c == ' ') {
                    // 非前导空格也用&nbsp;替换以确保保持
                    processedLine.append("&nbsp;");
                } else if (c == '\t') {
                    // 非前导制表符
                    processedLine.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                } else {
                    processedLine.append(c);
                }
            }
        }
        
        return processedLine.toString();
    }
    
    private String escapeHtmlButKeepFormatting(String text) {
        if (text == null) return "";
        
        // 详细分析原始文本
        int newlineCount = 0;
        int spaceCount = 0;
        int tabCount = 0;
        for (char c : text.toCharArray()) {
            if (c == '\n') newlineCount++;
            else if (c == ' ') spaceCount++;
            else if (c == '\t') tabCount++;
        }
        
        System.out.println("🔍 原始代码分析:");
        System.out.println("   总长度: " + text.length());
        System.out.println("   换行符数量: " + newlineCount);
        System.out.println("   空格数量: " + spaceCount);
        System.out.println("   制表符数量: " + tabCount);
        System.out.println("   前50字符: '" + text.substring(0, Math.min(50, text.length())).replace("\n", "\\n").replace("\t", "\\t") + "'");
        
        // 只转义必要的HTML特殊字符，完全保持空格、制表符、换行
        String result = text;
        result = result.replace("&", "&amp;");  // 必须第一个处理
        result = result.replace("<", "&lt;");
        result = result.replace(">", "&gt;");
        result = result.replace("\"", "&quot;");
        result = result.replace("'", "&#39;");
        
        // 验证转义后的结果
        int resultNewlineCount = 0;
        for (char c : result.toCharArray()) {
            if (c == '\n') resultNewlineCount++;
        }
        
        System.out.println("🔧 HTML转义完成:");
        System.out.println("   转义后长度: " + result.length());
        System.out.println("   转义后换行符: " + resultNewlineCount);
        System.out.println("   转义后前50字符: '" + result.substring(0, Math.min(50, result.length())).replace("\n", "\\n").replace("\t", "\\t") + "'");
        
        return result;
    }
    
    private String escapeHtmlPreserveSpaces(String text) {
        if (text == null) return "";
        // 保持所有空格和换行，只转义HTML特殊字符
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private String processLinesImproved(String html) {
        String[] lines = html.split("\\n");
        StringBuilder result = new StringBuilder();
        StringBuilder currentParagraph = new StringBuilder();
        boolean inCodeBlock = false;
        boolean inList = false;
        StringBuilder currentList = new StringBuilder();
        String currentListType = ""; // "ul" or "ol"
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 检查是否进入或退出代码块
            if (trimmedLine.startsWith("<pre>")) {
                inCodeBlock = true;
                // 结束当前段落
                if (currentParagraph.length() > 0) {
                    result.append("<p>").append(currentParagraph.toString().trim()).append("</p>\n");
                    currentParagraph.setLength(0);
                }
                result.append(line).append("\n");
                continue;
            } else if (trimmedLine.endsWith("</pre>")) {
                inCodeBlock = false;
                result.append(line).append("\n");
                continue;
            }
            
            // 如果在代码块内，直接添加（保持格式）
            if (inCodeBlock) {
                result.append(line).append("\n");
                continue;
            }
            
            // 检查是否是列表项
            if (trimmedLine.startsWith("<li>")) {
                // 结束当前段落
                if (currentParagraph.length() > 0) {
                    result.append("<p>").append(currentParagraph.toString().trim()).append("</p>\n");
                    currentParagraph.setLength(0);
                }
                
                // 确定列表类型
                String listType = (line.matches(".*^\\d+\\. .*")) ? "ol" : "ul";
                
                if (!inList) {
                    // 开始新列表
                    inList = true;
                    currentListType = listType;
                    currentList.setLength(0);
                    currentList.append("<").append(listType).append(">\n");
                } else if (!currentListType.equals(listType)) {
                    // 列表类型改变，结束当前列表，开始新列表
                    currentList.append("</").append(currentListType).append(">\n");
                    result.append(currentList.toString());
                    currentListType = listType;
                    currentList.setLength(0);
                    currentList.append("<").append(listType).append(">\n");
                }
                
                currentList.append(line).append("\n");
                continue;
            }
            
            // 如果不是列表项但在列表中，结束列表
            if (inList && !trimmedLine.startsWith("<li>")) {
                currentList.append("</").append(currentListType).append(">\n");
                result.append(currentList.toString());
                inList = false;
                currentList.setLength(0);
                currentListType = "";
            }
            
            // 如果是空行
            if (trimmedLine.isEmpty()) {
                // 结束当前段落
                if (currentParagraph.length() > 0) {
                    result.append("<p>").append(currentParagraph.toString().trim()).append("</p>\n");
                    currentParagraph.setLength(0);
                }
            }
            // 如果是HTML标签行（标题、引用、表格、分割线等）
            else if (trimmedLine.startsWith("<h") || trimmedLine.startsWith("<blockquote") || 
                     trimmedLine.startsWith("<table") || trimmedLine.startsWith("<hr") ||
                     trimmedLine.startsWith("<thead") || trimmedLine.startsWith("<tbody") ||
                     trimmedLine.startsWith("<tr") || trimmedLine.startsWith("<th") ||
                     trimmedLine.startsWith("<td") || trimmedLine.contains("</table>") ||
                     trimmedLine.contains("</thead>") || trimmedLine.contains("</tbody>") ||
                     trimmedLine.contains("</tr>")) {
                // 先结束当前段落
                if (currentParagraph.length() > 0) {
                    result.append("<p>").append(currentParagraph.toString().trim()).append("</p>\n");
                    currentParagraph.setLength(0);
                }
                // 直接添加HTML标签行
                result.append(line).append("\n");
            }
            // 普通文本行
            else {
                if (currentParagraph.length() > 0) {
                    currentParagraph.append(" ");
                }
                currentParagraph.append(trimmedLine);
            }
        }
        
        // 处理最后的列表
        if (inList) {
            currentList.append("</").append(currentListType).append(">\n");
            result.append(currentList.toString());
        }
        
        // 处理最后的段落
        if (currentParagraph.length() > 0) {
            result.append("<p>").append(currentParagraph.toString().trim()).append("</p>\n");
        }
        
        return result.toString();
    }
}
