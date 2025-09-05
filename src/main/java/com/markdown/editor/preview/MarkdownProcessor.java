package com.markdown.editor.preview;

import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.MarkdownParser;
import java.util.Stack;

/**
 * Markdown处理器
 * 负责将Markdown文本转换为HTML
 */
public class MarkdownProcessor {
    private String currentTheme = "暗黑";
    private final MarkdownParser parser;
    private final GFMFlavourDescriptor flavour;
    
    // 添加内存管理标记
    private volatile boolean disposed = false;
    
    public MarkdownProcessor() {
        this.flavour = new GFMFlavourDescriptor();
        this.parser = new MarkdownParser(flavour);
    }
    
    /**
     * 处理Markdown文本并转换为HTML (简化版本，兼容JTextPane)
     * @param markdownText 原始Markdown文本
     * @return 渲染后的HTML字符串
     */
    public String processMarkdown(String markdownText) {
        // 防御性检查：如果已释放则不处理
        if (disposed) {
            System.err.println("⚠️ MarkdownProcessor已释放，无法处理内容");
            return "<html><body><p>处理器已释放</p></body></html>";
        }
        
        if (markdownText == null || markdownText.trim().isEmpty()) {
            return "<html><body><p>请输入Markdown内容...</p></body></html>";
        }
        
        try {
            // 使用简化的HTML生成，避免复杂CSS
            String basicHtml = convertToSimpleHtml(markdownText);
            return "<html><body>" + basicHtml + "</body></html>";
            
        } catch (Exception e) {
            return "<html><body><p style='color: red;'>解析错误: " + e.getMessage() + "</p></body></html>";
        }
    }
    
    /**
     * 设置预览主题
     */
    public void setTheme(String theme) {
        this.currentTheme = theme;
    }
    
    /**
     * 转换为简单HTML，只使用基础标签，避免复杂CSS
     */
    private String convertToSimpleHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        String codeBlockLanguage = null;
        Stack<String> listStack = new Stack<>(); // 跟踪嵌套列表类型
        int lastListLevel = -1; // 跟踪列表层级
        
        for (String line : lines) {
            // 代码块处理
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</pre>\n");
                    inCodeBlock = false;
                    codeBlockLanguage = null;
                } else {
                    // 提取语言标识
                    codeBlockLanguage = line.substring(3).trim();
                    if (codeBlockLanguage.isEmpty()) {
                        codeBlockLanguage = "text";
                    }
                    
                    // 开始代码块，添加语言类名
                    html.append("<pre class=\"language-").append(codeBlockLanguage.toLowerCase()).append("\">");
                    inCodeBlock = true;
                }
                continue;
            }
            
            if (inCodeBlock) {
                // 在代码块中应用语法高亮
                String highlightedCode = applySyntaxHighlighting(line, codeBlockLanguage);
                html.append(highlightedCode).append("\n");
                continue;
            }
            
            // 标题处理
            if (line.startsWith("######")) {
                html.append("<h6>").append(processInlineFormatting(line.substring(6).trim())).append("</h6>\n");
            } else if (line.startsWith("#####")) {
                html.append("<h5>").append(processInlineFormatting(line.substring(5).trim())).append("</h5>\n");
            } else if (line.startsWith("####")) {
                html.append("<h4>").append(processInlineFormatting(line.substring(4).trim())).append("</h4>\n");
            } else if (line.startsWith("###")) {
                html.append("<h3>").append(processInlineFormatting(line.substring(3).trim())).append("</h3>\n");
            } else if (line.startsWith("##")) {
                html.append("<h2>").append(processInlineFormatting(line.substring(2).trim())).append("</h2>\n");
            } else if (line.startsWith("#")) {
                html.append("<h1>").append(processInlineFormatting(line.substring(1).trim())).append("</h1>\n");
            }
            // 列表处理 (支持多级嵌套)
            else if (line.matches("^\\s*[*+-]\\s+.*") || line.matches("^\\s*\\d+\\.\\s+.*")) {
                int currentLevel = getListLevel(line);
                boolean isOrdered = line.matches("^\\s*\\d+\\.\\s+.*");
                String listType = isOrdered ? "ol" : "ul";
                
                // 处理列表层级变化
                handleListLevelChange(html, listStack, lastListLevel, currentLevel, listType);
                
                // 添加列表项内容
                String content;
                if (isOrdered) {
                    content = line.replaceAll("^\\s*\\d+\\.\\s+", "");
                } else {
                    content = line.replaceAll("^\\s*[*+-]\\s+", "");
                }
                html.append("<li>").append(processInlineFormatting(content)).append("</li>\n");
                
                lastListLevel = currentLevel;
            }
            // 引用处理
            else if (line.startsWith(">")) {
                String content = line.substring(1).trim();
                html.append("<blockquote>").append(processInlineFormatting(content)).append("</blockquote>\n");
            }
            // 分隔线
            else if (line.matches("^\\s*[-*_]{3,}\\s*$")) {
                html.append("<hr>\n");
            }
            // 空行处理
            else if (line.trim().isEmpty()) {
                closeAllLists(html, listStack);
                lastListLevel = -1;
                html.append("<br>\n");
            }
            // 普通段落
            else {
                closeAllLists(html, listStack);
                lastListLevel = -1;
                html.append("<p>").append(processInlineFormatting(line)).append("</p>\n");
            }
        }
        
        // 关闭未闭合的标签
        if (inCodeBlock) {
            html.append("</pre>\n");
        }
        closeAllLists(html, listStack);
        
        return html.toString();
    }
    
    /**
     * 处理行内格式（粗体、斜体、链接等）
     */
    private String processInlineFormatting(String text) {
        if (text == null) return "";
        
        // 先进行HTML转义
        text = escapeHtml(text);
        
        // 然后处理Markdown格式，注意这里需要处理已转义的字符
        
        // 处理链接 [text](url)
        text = text.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\">$1</a>");
        
        // 处理粗体 **text**
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        
        // 处理斜体 *text*
        text = text.replaceAll("\\*([^*]+)\\*", "<em>$1</em>");
        
        // 处理行内代码 `code`
        text = text.replaceAll("`([^`]+)`", "<code>$1</code>");
        
        return text;
    }
    
    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * 获取列表项的缩进层级
     */
    private int getListLevel(String line) {
        int spaces = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                spaces++;
            } else if (c == '\t') {
                spaces += 4; // 一个tab等于4个空格
            } else {
                break;
            }
        }
        return spaces / 2; // 每两个空格为一个层级
    }
    
    /**
     * 处理列表层级变化
     */
    private void handleListLevelChange(StringBuilder html, Stack<String> listStack, 
                                      int lastLevel, int currentLevel, String listType) {
        // 如果当前层级比上一层级深，需要开始新的嵌套列表
        if (currentLevel > lastLevel) {
            for (int i = lastLevel + 1; i <= currentLevel; i++) {
                html.append("<").append(listType).append(">\n");
                listStack.push(listType);
            }
        }
        // 如果当前层级比上一层级浅，需要关闭一些列表
        else if (currentLevel < lastLevel) {
            int levelsToClose = lastLevel - currentLevel;
            for (int i = 0; i < levelsToClose && !listStack.isEmpty(); i++) {
                String closingType = listStack.pop();
                html.append("</").append(closingType).append(">\n");
            }
            
            // 如果列表类型不同，需要关闭当前列表并开始新的
            if (!listStack.isEmpty() && !listStack.peek().equals(listType)) {
                String oldType = listStack.pop();
                html.append("</").append(oldType).append(">\n");
                html.append("<").append(listType).append(">\n");
                listStack.push(listType);
            } else if (listStack.isEmpty()) {
                html.append("<").append(listType).append(">\n");
                listStack.push(listType);
            }
        }
        // 同一层级，但列表类型不同
        else if (currentLevel == lastLevel && !listStack.isEmpty() && !listStack.peek().equals(listType)) {
            String oldType = listStack.pop();
            html.append("</").append(oldType).append(">\n");
            html.append("<").append(listType).append(">\n");
            listStack.push(listType);
        }
        // 第一个列表项
        else if (listStack.isEmpty()) {
            html.append("<").append(listType).append(">\n");
            listStack.push(listType);
        }
    }
    
    /**
     * 关闭所有打开的列表
     */
    private void closeAllLists(StringBuilder html, Stack<String> listStack) {
        while (!listStack.isEmpty()) {
            String listType = listStack.pop();
            html.append("</").append(listType).append(">\n");
        }
    }
    
    /**
     * 应用语法高亮 (修复重复标签问题)
     */
    private String applySyntaxHighlighting(String line, String language) {
        if (line == null || line.trim().isEmpty()) {
            return escapeHtml(line);
        }
        
        // 根据语言类型应用不同的高亮规则 (在转义前处理，避免标签冲突)
        switch (language.toLowerCase()) {
            case "java":
                return highlightJavaSimple(line);
            case "javascript":
            case "js":
                return highlightJavaScriptSimple(line);
            case "python":
            case "py":
                return highlightPythonSimple(line);
            case "html":
            case "xml":
                return highlightHtmlSimple(line);
            case "css":
                return highlightCssSimple(line);
            case "json":
                return highlightJsonSimple(line);
            case "sql":
                return highlightSqlSimple(line);
            default:
                return escapeHtml(line); // 无高亮，直接返回转义后的文本
        }
    }
    
    /**
     * 简化的Java语法高亮 (直接方式，避免临时标记)
     */
    private String highlightJavaSimple(String line) {
        if (line == null || line.trim().isEmpty()) {
            return escapeHtml(line);
        }
        
        // 先转义HTML特殊字符
        line = escapeHtml(line);
        
        // 直接进行替换，按优先级顺序
        
        // 1. 处理行注释 (优先级最高)
        if (line.contains("//")) {
            int commentIndex = line.indexOf("//");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            return processJavaCode(beforeComment) + "<span class=\"comment\">" + comment + "</span>";
        }
        
        // 2. 处理多行注释
        line = line.replaceAll("/\\*([^*]*)\\*/", "<span class=\"comment\">/*$1*/</span>");
        
        // 3. 处理字符串 (在关键字之前)
        line = line.replaceAll("\"([^\"]*)\"", "<span class=\"string\">\"$1\"</span>");
        line = line.replaceAll("'([^']*)'", "<span class=\"string\">'$1'</span>");
        
        // 4. 处理剩余的代码部分
        return processJavaCode(line);
    }
    
    /**
     * 处理Java代码的关键字和数字高亮
     */
    private String processJavaCode(String line) {
        // 处理数字
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span class=\"number\">$1</span>");
        
        // 处理关键字
        String[] javaKeywords = {"public", "private", "protected", "static", "final", "abstract", 
                                "class", "interface", "extends", "implements", "import", "package",
                                "if", "else", "for", "while", "do", "switch", "case", "default",
                                "break", "continue", "return", "try", "catch", "finally", "throw", "throws",
                                "new", "this", "super", "null", "true", "false", "void", "int", "String",
                                "boolean", "double", "float", "long", "char", "byte", "short"};
        
        for (String keyword : javaKeywords) {
            // 只替换完整的单词，避免替换HTML标签中的内容
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", "<span class=\"keyword\">" + keyword + "</span>");
        }
        
        return line;
    }
    
    /**
     * 简化的JavaScript语法高亮
     */
    private String highlightJavaScriptSimple(String line) {
        if (line == null || line.trim().isEmpty()) {
            return escapeHtml(line);
        }
        
        line = escapeHtml(line);
        
        // 处理行注释
        if (line.contains("//")) {
            int commentIndex = line.indexOf("//");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            return processJSCode(beforeComment) + "<span class=\"comment\">" + comment + "</span>";
        }
        
        // 处理多行注释
        line = line.replaceAll("/\\*([^*]*)\\*/", "<span class=\"comment\">/*$1*/</span>");
        
        // 处理字符串
        line = line.replaceAll("\"([^\"]*)\"", "<span class=\"string\">\"$1\"</span>");
        line = line.replaceAll("'([^']*)'", "<span class=\"string\">'$1'</span>");
        line = line.replaceAll("`([^`]*)`", "<span class=\"string\">`$1`</span>"); // 模板字符串
        
        return processJSCode(line);
    }
    
    /**
     * 处理JavaScript代码的关键字和数字高亮
     */
    private String processJSCode(String line) {
        // 处理数字
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span class=\"number\">$1</span>");
        
        // 处理关键字
        String[] jsKeywords = {"function", "var", "let", "const", "if", "else", "for", "while", 
                              "do", "switch", "case", "default", "break", "continue", "return",
                              "try", "catch", "finally", "throw", "new", "this", "typeof", "instanceof",
                              "true", "false", "null", "undefined", "class", "extends"};
        
        for (String keyword : jsKeywords) {
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", "<span class=\"keyword\">" + keyword + "</span>");
        }
        
        return line;
    }
    
    /**
     * 简化的Python语法高亮
     */
    private String highlightPythonSimple(String line) {
        if (line == null || line.trim().isEmpty()) {
            return escapeHtml(line);
        }
        
        line = escapeHtml(line);
        
        // 处理Python注释
        if (line.contains("#")) {
            int commentIndex = line.indexOf("#");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            return processPythonCode(beforeComment) + "<span class=\"comment\">" + comment + "</span>";
        }
        
        // 处理字符串
        line = line.replaceAll("\"([^\"]*)\"", "<span class=\"string\">\"$1\"</span>");
        line = line.replaceAll("'([^']*)'", "<span class=\"string\">'$1'</span>");
        
        return processPythonCode(line);
    }
    
    /**
     * 处理Python代码的关键字和数字高亮
     */
    private String processPythonCode(String line) {
        // 处理数字
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span class=\"number\">$1</span>");
        
        // 处理关键字
        String[] pythonKeywords = {"def", "class", "if", "elif", "else", "for", "while", "break", 
                                  "continue", "return", "try", "except", "finally", "raise",
                                  "import", "from", "as", "with", "pass", "lambda", "yield",
                                  "True", "False", "None", "and", "or", "not", "in", "is"};
        
        for (String keyword : pythonKeywords) {
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", "<span class=\"keyword\">" + keyword + "</span>");
        }
        
        return line;
    }
    
    /**
     * 简化的HTML语法高亮
     */
    private String highlightHtmlSimple(String line) {
        line = escapeHtml(line);
        
        // HTML标签 (已转义的)
        line = line.replaceAll("&lt;(/?)([a-zA-Z][a-zA-Z0-9]*)(.*?)&gt;", 
                              "<span class=\"keyword\">&lt;$1$2</span><span class=\"variable\">$3</span><span class=\"keyword\">&gt;</span>");
        
        // HTML属性
        line = line.replaceAll("([a-zA-Z-]+)=&quot;([^&]*)&quot;", 
                              "<span class=\"function\">$1</span>=<span class=\"string\">&quot;$2&quot;</span>");
        
        return line;
    }
    
    /**
     * 简化的CSS语法高亮
     */
    private String highlightCssSimple(String line) {
        line = escapeHtml(line);
        
        // CSS选择器
        line = line.replaceAll("^([.#]?[a-zA-Z][a-zA-Z0-9_-]*)", "<span class=\"type\">$1</span>");
        
        // CSS属性
        line = line.replaceAll("([a-zA-Z-]+):", "<span class=\"function\">$1</span>:");
        
        // CSS值
        line = line.replaceAll(":([^;]+);", ": <span class=\"string\">$1</span>;");
        
        return line;
    }
    
    /**
     * 简化的JSON语法高亮
     */
    private String highlightJsonSimple(String line) {
        line = escapeHtml(line);
        
        // JSON键
        line = line.replaceAll("&quot;([^&]+?)&quot;:", "<span class=\"function\">&quot;$1&quot;</span>:");
        
        // JSON字符串值
        line = line.replaceAll(":&quot;([^&]*)&quot;", ": <span class=\"string\">&quot;$1&quot;</span>");
        
        // JSON数字、布尔值、null
        line = line.replaceAll("\\b(true|false|null)\\b", "<span class=\"keyword\">$1</span>");
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span class=\"number\">$1</span>");
        
        return line;
    }
    
    /**
     * 简化的SQL语法高亮
     */
    private String highlightSqlSimple(String line) {
        line = escapeHtml(line);
        
        // 处理字符串
        line = line.replaceAll("'([^']*)'", "<span class=\"string\">'$1'</span>");
        
        // 处理数字
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span class=\"number\">$1</span>");
        
        // 处理关键字
        String[] sqlKeywords = {"SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", 
                               "TABLE", "INDEX", "DROP", "ALTER", "JOIN", "INNER", "LEFT", "RIGHT",
                               "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION",
                               "AND", "OR", "NOT", "NULL", "TRUE", "FALSE", "AS", "DISTINCT"};
        
        for (String keyword : sqlKeywords) {
            line = line.replaceAll("\\b" + keyword.toLowerCase() + "\\b(?![^<]*>)", "<span class=\"keyword\">" + keyword.toLowerCase() + "</span>");
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", "<span class=\"keyword\">" + keyword + "</span>");
        }
        
        return line;
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
        // 只使用暗黑主题
                return getDarkThemeCss();
    }
    
    
    private String getDarkThemeCss() {
        return 
            "* {" +
            "  border: none !important;" +  // 强制移除所有默认边框
            "}" +
            "body {" +
            "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;" +
            "  font-size: 13px;" +
            "  color: #c9d1d9;" +
            "  background-color: #0d1117 !important;" +
            "  margin: 16px;" +
            "  line-height: 1.6;" +
            "  border: none !important;" +
            "}" +
            "h1 {" +
            "  font-size: 22px;" +
            "  color: #f0f6fc;" +
            "  border-bottom: 1px solid #30363d !important;" +
            "  border-top: none !important;" +
            "  border-left: none !important;" +
            "  border-right: none !important;" +
            "  margin: 24px 0 16px 0;" +
            "  padding-bottom: 8px;" +
            "  font-weight: 600;" +
            "}" +
            "h2 {" +
            "  font-size: 18px;" +
            "  color: #f0f6fc;" +
            "  border-bottom: 1px solid #30363d !important;" +
            "  border-top: none !important;" +
            "  border-left: none !important;" +
            "  border-right: none !important;" +
            "  margin: 20px 0 12px 0;" +
            "  padding-bottom: 6px;" +
            "  font-weight: 600;" +
            "}" +
            "h3 {" +
            "  font-size: 16px;" +
            "  color: #f0f6fc;" +
            "  margin: 16px 0 8px 0;" +
            "  border: none !important;" +
            "  font-weight: 600;" +
            "}" +
            "h4 {" +
            "  font-size: 14px;" +
            "  color: #f0f6fc;" +
            "  margin: 12px 0 6px 0;" +
            "  border: none !important;" +
            "  font-weight: 600;" +
            "}" +
            "h5, h6 {" +
            "  font-size: 13px;" +
            "  color: #f0f6fc;" +
            "  margin: 10px 0 4px 0;" +
            "  border: none !important;" +
            "  font-weight: 600;" +
            "}" +
            "p {" +
            "  margin-bottom: 12px;" +
            "  line-height: 1.6;" +
            "  border: none !important;" +
            "}" +
            "code {" +
            "  font-family: Consolas, monospace;" +
            "  background-color: #161b22;" +
            "  color: #e6edf3;" +
            "  padding: 2px 4px;" +
            "  border: 1px solid #30363d;" +
            "  font-size: 13px;" +
            "}" +
            "pre {" +
            "  font-family: Consolas, monospace;" +
            "  background-color: #161b22;" +
            "  color: #e6edf3;" +
            "  border: 1px solid #30363d;" +
            "  padding: 16px;" +
            "  margin: 16px 0;" +
            "  white-space: pre;" +
            "  font-size: 14px;" +
            "  line-height: 1.4;" +
            "}" +
            "blockquote {" +
            "  border-left: 4px solid #30363d !important;" +
            "  border-top: none !important;" +
            "  border-right: none !important;" +
            "  border-bottom: none !important;" +
            "  padding-left: 16px;" +
            "  margin: 12px 0;" +
            "  color: #8b949e;" +
            "  font-style: italic;" +
            "}" +
            "ul {" +
            "  margin: 12px 0;" +
            "  padding-left: 24px;" +
            "  list-style-type: disc;" +
            "  border: none !important;" +
            "}" +
            "ol {" +
            "  margin: 12px 0;" +
            "  padding-left: 24px;" +
            "  list-style-type: decimal;" +
            "  border: none !important;" +
            "}" +
            "ul ul {" +
            "  margin: 4px 0;" +
            "  padding-left: 20px;" +
            "  list-style-type: circle;" +
            "}" +
            "ul ul ul {" +
            "  list-style-type: square;" +
            "}" +
            "ol ol {" +
            "  margin: 4px 0;" +
            "  padding-left: 20px;" +
            "  list-style-type: lower-alpha;" +
            "}" +
            "ol ol ol {" +
            "  list-style-type: lower-roman;" +
            "}" +
            "/* 混合嵌套样式 */" +
            "ul ol {" +
            "  margin: 4px 0;" +
            "  padding-left: 20px;" +
            "  list-style-type: decimal;" +
            "}" +
            "ol ul {" +
            "  margin: 4px 0;" +
            "  padding-left: 20px;" +
            "  list-style-type: disc;" +
            "}" +
            "ul ol ul {" +
            "  list-style-type: circle;" +
            "}" +
            "ol ul ol {" +
            "  list-style-type: lower-alpha;" +
            "}" +
            "li {" +
            "  margin: 3px 0;" +
            "  line-height: 1.5;" +
            "  border: none !important;" +
            "}" +
            "li p {" +
            "  margin: 0;" +
            "}" +
            "/* 确保列表项内容对齐 */" +
            "li > ul, li > ol {" +
            "  margin-top: 4px;" +
            "  margin-bottom: 4px;" +
            "}" +
            "table {" +
            "  border-collapse: collapse;" +
            "  width: 100%;" +
            "  margin: 16px 0;" +
            "  border: none !important;" +
            "  background-color: transparent;" +
            "}" +
            "th, td {" +
            "  border: 1px solid #30363d !important;" +
            "  padding: 8px 12px;" +
            "  text-align: left;" +
            "  color: #c9d1d9;" +
            "  vertical-align: top;" +
            "}" +
            "th {" +
            "  background-color: #21262d;" +
            "  font-weight: 600;" +
            "  color: #f0f6fc;" +
            "}" +
            "tr:nth-child(even) {" +
            "  background-color: #161b22;" +
            "}" +
            "strong {" +
            "  font-weight: 600;" +
            "  color: #f0f6fc;" +
            "  border: none !important;" +
            "}" +
            "em {" +
            "  font-style: italic;" +
            "  color: #e6edf3;" +
            "  border: none !important;" +
            "}" +
            "a {" +
            "  color: #58a6ff;" +
            "  text-decoration: underline;" +
            "  border: none !important;" +
            "}" +
            "a:hover {" +
            "  color: #79c0ff;" +
            "  text-decoration: none;" +
            "}" +
            "hr {" +
            "  border: none;" +
            "  height: 1px;" +
            "  background-color: #30363d;" +
            "  margin: 24px 0;" +
            "}" +
            "img {" +
            "  max-width: 100%;" +
            "  height: auto;" +
            "  border: 1px solid #30363d;" +
            "}" +
            "del {" +
            "  text-decoration: line-through;" +
            "  color: #8b949e;" +
            "}";
    }
    
    
    private String getJavaScript() {
        // JEditorPane 对 JavaScript 支持有限，简化或移除
        return "";
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
        
        // 处理代码块（简化版本，兼容JEditorPane）
        html = processCodeBlocksSimple(html);
        
        // 处理行内代码（在代码块之后处理）
        html = html.replaceAll("`([^`]+?)`", "<code>$1</code>");
        
        // 处理链接
        html = html.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href=\"$2\">$1</a>");
        
        // 处理图片
        html = html.replaceAll("!\\[(.+?)\\]\\((.+?)\\)", "<img src=\"$2\" alt=\"$1\" />");
        
        // 处理引用
        html = html.replaceAll("(?m)^> (.+)$", "<blockquote>$1</blockquote>");
        
        // 处理列表（支持多级嵌套）
        html = processNestedLists(html);
        
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
    
    private String processNestedLists(String html) {
        String[] lines = html.split("\\n");
        StringBuilder result = new StringBuilder();
        java.util.List<ListLevel> listStack = new java.util.ArrayList<>();
        
        for (String line : lines) {
            if (isListItem(line)) {
                processListItem(result, line, listStack);
            } else {
                // 不是列表项，关闭所有打开的列表
                closeAllLists(result, listStack);
                result.append(line).append("\n");
            }
        }
        
        // 关闭剩余的列表
        closeAllLists(result, listStack);
        
        return result.toString();
    }
    
    private boolean isListItem(String line) {
        return line.matches("^( *)(\\d+\\.|[-*+]) (.+)$");
    }
    
    private void processListItem(StringBuilder result, String line, java.util.List<ListLevel> listStack) {
        // 计算缩进级别（每4个空格为一级，兼容2个空格）
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') indent++;
            else break;
        }
        int level = Math.max(0, indent / 2); // 每2个空格为一级
        
        // 提取列表内容和类型
        String listContent = line.replaceAll("^( *)(\\d+\\.|[-*+]) (.+)$", "$3");
        boolean isOrdered = line.matches("^( *)\\d+\\. (.+)$");
        String listType = isOrdered ? "ol" : "ul";
        
        // 处理列表层级
        adjustListStack(result, listStack, level, listType);
        
        // 添加列表项
        result.append("<li>").append(listContent).append("</li>\n");
    }
    
    private void adjustListStack(StringBuilder result, java.util.List<ListLevel> listStack, int level, String listType) {
        // 关闭比当前级别深的列表
        while (!listStack.isEmpty() && listStack.get(listStack.size() - 1).level > level) {
            ListLevel closingLevel = listStack.remove(listStack.size() - 1);
            result.append("</").append(closingLevel.type).append(">\n");
        }
        
        // 检查当前级别
        if (listStack.isEmpty() || listStack.get(listStack.size() - 1).level < level) {
            // 需要开始新的更深层级的列表
            result.append("<").append(listType).append(">\n");
            listStack.add(new ListLevel(level, listType));
        } else if (listStack.get(listStack.size() - 1).level == level) {
            // 同级别，检查类型是否相同
            ListLevel currentLevel = listStack.get(listStack.size() - 1);
            if (!currentLevel.type.equals(listType)) {
                // 类型不同，关闭当前列表，开始新列表
                listStack.remove(listStack.size() - 1);
                result.append("</").append(currentLevel.type).append(">\n");
                result.append("<").append(listType).append(">\n");
                listStack.add(new ListLevel(level, listType));
            }
            // 如果类型相同，继续使用当前列表
        }
    }
    
    private void closeAllLists(StringBuilder result, java.util.List<ListLevel> listStack) {
        // 从最深层开始关闭所有列表
        for (int i = listStack.size() - 1; i >= 0; i--) {
            ListLevel level = listStack.get(i);
            result.append("</").append(level.type).append(">\n");
        }
        listStack.clear();
    }
    
    // 辅助类来存储列表层级信息
    private static class ListLevel {
        final int level;
        final String type;
        
        ListLevel(int level, String type) {
            this.level = level;
            this.type = type;
        }
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
        StringBuilder result = new StringBuilder();
        int index = 0;
        
        while (index < html.length()) {
            int codeStart = html.indexOf("```", index);
            if (codeStart == -1) {
                result.append(html.substring(index));
                break;
            }
            
            result.append(html.substring(index, codeStart));
            
            int lineEnd = html.indexOf('\n', codeStart);
            if (lineEnd == -1) {
                result.append(html.substring(codeStart));
                break;
            }
            
            // 提取语言标识
            String language = html.substring(codeStart + 3, lineEnd).trim().toLowerCase();
            
            int codeEnd = html.indexOf("\n```", lineEnd);
            if (codeEnd == -1) {
                String code = html.substring(lineEnd + 1);
                result.append(createHighlightedCodeBlock(code, language));
                break;
            }
            
            String code = html.substring(lineEnd + 1, codeEnd);
            result.append(createHighlightedCodeBlock(code, language));
            
            index = codeEnd + 4;
        }
        
        return result.toString();
    }
    
    /**
     * 创建简单的代码块HTML（兼容JEditorPane）
     */
    private String createSimpleCodeBlock(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "<pre></pre>";
        }
        
        // 简单的HTML转义，保持原始空格和制表符
        String escapedCode = code.replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        
        return "<pre>" + escapedCode + "</pre>";
    }
    
    /**
     * 创建带语法高亮的代码块HTML
     */
    private String createHighlightedCodeBlock(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            return "<pre></pre>";
        }
        
        // 分行处理，添加语法高亮
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            String highlightedLine = applySyntaxHighlighting(line, language, currentTheme);
            result.append(highlightedLine);
            if (lines.length > 1) {
                result.append("\n");
            }
        }
        
        return "<pre>" + result.toString() + "</pre>";
    }
    
    /**
     * 为单行代码应用语法高亮
     */
    private String applySyntaxHighlighting(String line, String language, String theme) {
        if (line == null) {
            return "";
        }
        
        // 如果是空行，返回空行
        if (line.trim().isEmpty()) {
            return line;
        }
        
        // 先处理制表符和前导空格，保持缩进
        StringBuilder processedLine = new StringBuilder();
        int i = 0;
        
        // 处理前导空格和制表符
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == ' ') {
                processedLine.append("&nbsp;"); // 保持空格
                i++;
            } else if (c == '\t') {
                processedLine.append("&nbsp;&nbsp;&nbsp;&nbsp;"); // 制表符转换为4个空格
                i++;
            } else {
                break;
            }
        }
        
        // 处理剩余内容
        String remaining = line.substring(i);
        
        // HTML转义剩余内容
        remaining = remaining.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
        
        processedLine.append(remaining);
        
        // 根据语言应用语法高亮
        return highlightByLanguage(processedLine.toString(), language, theme);
    }
    
    /**
     * 根据语言应用语法高亮
     */
    private String highlightByLanguage(String line, String language, String theme) {
        if (language == null) language = "";
        
        // 始终使用暗黑主题
        boolean isDark = true;
        
        switch (language.toLowerCase()) {
            case "java":
                return highlightJava(line, isDark);
            case "javascript":
            case "js":
                return highlightJavaScript(line, isDark);
            case "python":
            case "py":
                return highlightPython(line, isDark);
            case "html":
                return highlightHtml(line, isDark);
            case "css":
                return highlightCss(line, isDark);
            case "json":
                return highlightJson(line, isDark);
            default:
                return highlightGeneric(line, isDark);
        }
    }
    
    /**
     * Java语法高亮
     */
    private String highlightJava(String line, boolean isDark) {
        // Java关键字
        String[] keywords = {"public", "private", "protected", "static", "final", "class", "interface", 
                           "extends", "implements", "import", "package", "void", "int", "String", "boolean",
                           "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return",
                           "new", "this", "super", "try", "catch", "finally", "throw", "throws"};
        
        String keywordColor = isDark ? "#ff7b72" : "#cf222e";  // 红色 - GitHub主题使用更鲜明的红色
        String stringColor = isDark ? "#a5d6ff" : "#0a3069";   // 蓝色 - GitHub主题使用更深的蓝色确保对比度
        String commentColor = isDark ? "#8b949e" : "#6a737d";  // 灰色 - 保持原色
        
        for (String keyword : keywords) {
            line = line.replaceAll("\\b" + keyword + "\\b", 
                "<span style='color: " + keywordColor + "; font-weight: bold;'>" + keyword + "</span>");
        }
        
        // 字符串高亮
        line = line.replaceAll("\"([^\"]*?)\"", 
            "<span style='color: " + stringColor + ";'>\"$1\"</span>");
        
        // 注释高亮
        line = line.replaceAll("//(.+)$", 
            "<span style='color: " + commentColor + "; font-style: italic;'>//$1</span>");
        
        return line;
    }
    
    /**
     * JavaScript语法高亮
     */
    private String highlightJavaScript(String line, boolean isDark) {
        String[] keywords = {"var", "let", "const", "function", "return", "if", "else", "for", "while", 
                           "do", "switch", "case", "break", "continue", "true", "false", "null", "undefined"};
        
        String keywordColor = isDark ? "#ff7b72" : "#cf222e";
        String stringColor = isDark ? "#a5d6ff" : "#0a3069";
        
        for (String keyword : keywords) {
            line = line.replaceAll("\\b" + keyword + "\\b", 
                "<span style='color: " + keywordColor + "; font-weight: bold;'>" + keyword + "</span>");
        }
        
        // 字符串高亮
        line = line.replaceAll("\"([^\"]*?)\"", 
            "<span style='color: " + stringColor + ";'>\"$1\"</span>");
        line = line.replaceAll("'([^']*?)'", 
            "<span style='color: " + stringColor + ";'>'$1'</span>");
        
        return line;
    }
    
    /**
     * Python语法高亮
     */
    private String highlightPython(String line, boolean isDark) {
        String[] keywords = {"def", "class", "import", "from", "if", "elif", "else", "for", "while", 
                           "try", "except", "finally", "return", "yield", "pass", "break", "continue",
                           "True", "False", "None", "and", "or", "not", "in", "is"};
        
        String keywordColor = isDark ? "#ff7b72" : "#cf222e";
        String stringColor = isDark ? "#a5d6ff" : "#0a3069";
        String commentColor = isDark ? "#8b949e" : "#6a737d";
        
        for (String keyword : keywords) {
            line = line.replaceAll("\\b" + keyword + "\\b", 
                "<span style='color: " + keywordColor + "; font-weight: bold;'>" + keyword + "</span>");
        }
        
        // 字符串高亮
        line = line.replaceAll("\"([^\"]*?)\"", 
            "<span style='color: " + stringColor + ";'>\"$1\"</span>");
        line = line.replaceAll("'([^']*?)'", 
            "<span style='color: " + stringColor + ";'>'$1'</span>");
        
        // 注释高亮
        line = line.replaceAll("#(.+)$", 
            "<span style='color: " + commentColor + "; font-style: italic;'>#$1</span>");
        
        return line;
    }
    
    /**
     * HTML语法高亮
     */
    private String highlightHtml(String line, boolean isDark) {
        String tagColor = isDark ? "#7ee787" : "#116329";  // 绿色 - GitHub主题使用更深的绿色
        
        // HTML标签高亮
        line = line.replaceAll("&lt;([^&gt;]+)&gt;", 
            "<span style='color: " + tagColor + "; font-weight: bold;'>&lt;$1&gt;</span>");
        
        return line;
    }
    
    /**
     * CSS语法高亮
     */
    private String highlightCss(String line, boolean isDark) {
        String propertyColor = isDark ? "#ff7b72" : "#cf222e";
        
        // CSS属性高亮
        line = line.replaceAll("([a-zA-Z-]+):", 
            "<span style='color: " + propertyColor + ";'>$1</span>:");
        
        return line;
    }
    
    /**
     * JSON语法高亮
     */
    private String highlightJson(String line, boolean isDark) {
        String keyColor = isDark ? "#79c0ff" : "#0a3069";
        String valueColor = isDark ? "#a5d6ff" : "#0a3069";
        
        // JSON字符串高亮
        line = line.replaceAll("\"([^\"]*?)\":", 
            "<span style='color: " + keyColor + "; font-weight: bold;'>\"$1\"</span>:");
        line = line.replaceAll(":&nbsp;\"([^\"]*?)\"", 
            ": <span style='color: " + valueColor + ";'>\"$1\"</span>");
        
        return line;
    }
    
    /**
     * 通用语法高亮
     */
    private String highlightGeneric(String line, boolean isDark) {
        String stringColor = isDark ? "#a5d6ff" : "#0a3069";
        String numberColor = isDark ? "#79c0ff" : "#0969da";
        
        // 字符串高亮
        line = line.replaceAll("\"([^\"]*?)\"", 
            "<span style='color: " + stringColor + ";'>\"$1\"</span>");
        line = line.replaceAll("'([^']*?)'", 
            "<span style='color: " + stringColor + ";'>'$1'</span>");
        
        // 数字高亮
        line = line.replaceAll("\\b(\\d+)\\b", 
            "<span style='color: " + numberColor + ";'>$1</span>");
        
        return line;
    }
    
    
    private String processLinesImproved(String html) {
        String[] lines = html.split("\\n");
        StringBuilder result = new StringBuilder();
        StringBuilder currentParagraph = new StringBuilder();
        boolean inCodeBlock = false;
        
        // 处理嵌套列表
        result.append(processNestedListHTML(lines));
        
        return result.toString();
    }
    
    private String processNestedListHTML(String[] lines) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentParagraph = new StringBuilder();
        boolean inCodeBlock = false;
        boolean inList = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 检查是否进入或退出代码块
            if (trimmedLine.startsWith("<pre>")) {
                inCodeBlock = true;
                finalizeParagraph(result, currentParagraph);
                result.append(line).append("\n");
                continue;
            } else if (trimmedLine.endsWith("</pre>")) {
                inCodeBlock = false;
                result.append(line).append("\n");
                continue;
            }
            
            // 如果在代码块内，直接添加
            if (inCodeBlock) {
                result.append(line).append("\n");
                continue;
            }
            
            // 检查是否是列表相关的HTML标签
            if (trimmedLine.startsWith("<ul>") || trimmedLine.startsWith("<ol>") || 
                trimmedLine.startsWith("<li>") || trimmedLine.equals("</ul>") || 
                trimmedLine.equals("</ol>") || trimmedLine.equals("</li>")) {
                finalizeParagraph(result, currentParagraph);
                result.append(line).append("\n");
                inList = trimmedLine.startsWith("<ul>") || trimmedLine.startsWith("<ol>") || inList;
                if (trimmedLine.equals("</ul>") || trimmedLine.equals("</ol>")) {
                    inList = false;
                }
                continue;
            }
            
            // 处理其他HTML标签
            if (trimmedLine.isEmpty()) {
                if (!inList) { // 在列表中不创建空段落
                    finalizeParagraph(result, currentParagraph);
                }
            } else if (trimmedLine.startsWith("<h") || trimmedLine.startsWith("<blockquote") || 
                     trimmedLine.startsWith("<table") || trimmedLine.startsWith("<hr") ||
                     trimmedLine.startsWith("<thead") || trimmedLine.startsWith("<tbody") ||
                     trimmedLine.startsWith("<tr") || trimmedLine.startsWith("<th") ||
                     trimmedLine.startsWith("<td") || trimmedLine.contains("</table>") ||
                     trimmedLine.contains("</thead>") || trimmedLine.contains("</tbody>") ||
                     trimmedLine.contains("</tr>")) {
                finalizeParagraph(result, currentParagraph);
                result.append(line).append("\n");
            } else {
            // 普通文本行
                if (!inList) { // 只有在非列表状态下才合并段落
                if (currentParagraph.length() > 0) {
                    currentParagraph.append(" ");
                }
                currentParagraph.append(trimmedLine);
                } else {
                    // 在列表中，直接输出
                    result.append(line).append("\n");
                }
            }
        }
        
        // 完成最后的段落
        finalizeParagraph(result, currentParagraph);
        
        return result.toString();
    }
    
    private void finalizeParagraph(StringBuilder result, StringBuilder currentParagraph) {
        if (currentParagraph.length() > 0) {
            result.append("<p>").append(currentParagraph.toString().trim()).append("</p>\n");
            currentParagraph.setLength(0);
        }
    }
    
    /**
     * 释放处理器资源
     */
    public void dispose() {
        System.out.println("🗑️ 释放MarkdownProcessor资源");
        
        try {
            // 标记为已释放
            disposed = true;
            
            // 清空主题设置
            currentTheme = null;
            
            // 注意：parser和flavour是final的，让GC自动回收
            
            System.out.println("✅ MarkdownProcessor资源释放完成");
            
        } catch (Exception e) {
            System.err.println("❌ 释放MarkdownProcessor资源时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
