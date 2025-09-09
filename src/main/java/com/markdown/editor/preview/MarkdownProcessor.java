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
    private final MarkdownParser parser;
    private final GFMFlavourDescriptor flavour;
    
    // 添加内存管理标记
    private volatile boolean disposed = false;
    
    // 代码块折叠状态管理
    private final java.util.Map<String, Boolean> codeBlockFoldStates = new java.util.concurrent.ConcurrentHashMap<>();
    
    
    // 性能优化：预编译正则表达式
    private static final java.util.regex.Pattern INLINE_CODE_PATTERN = java.util.regex.Pattern.compile("`([^`]*)`");
    private static final java.util.regex.Pattern BOLD_PATTERN = java.util.regex.Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final java.util.regex.Pattern ITALIC_PATTERN = java.util.regex.Pattern.compile("\\*([^*]+)\\*");
    private static final java.util.regex.Pattern LINK_PATTERN = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    
    // 性能优化：常用字符串常量
    private static final String COMMENT_STYLE = "color: #6A9955; font-style: italic;";
    private static final String STRING_STYLE = "color: #CE9178;";
    private static final String KEYWORD_STYLE = "color: #569CD6; font-weight: bold;";
    private static final String NUMBER_STYLE = "color: #B5CEA8;";
    
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
            String result = "<html><body>" + basicHtml + "</body></html>";
            
            return result;
            
        } catch (Exception e) {
            return "<html><body><p style='color: red;'>解析错误: " + e.getMessage() + "</p></body></html>";
        }
    }
    
    
    /**
     * 转换为简单HTML，只使用基础标签，避免复杂CSS
     */
    private String convertToSimpleHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        
        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        boolean inTable = false;
        String codeBlockLanguage = null;
        String currentCodeBlockId = null; // 当前代码块ID
        Stack<String> listStack = new Stack<>(); // 跟踪嵌套列表类型
        int lastListLevel = -1; // 跟踪列表层级
        int codeBlockIndex = 0; // 代码块索引，基于文档位置
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // 代码块处理 (支持折叠)
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</code></pre>");
                    if (currentCodeBlockId != null) {
                        html.append("</div></div>\n");
                        currentCodeBlockId = null;
                    }
                    inCodeBlock = false;
                    codeBlockLanguage = null;
                } else {
                    // 提取语言标识
                    codeBlockLanguage = line.substring(3).trim();
                    if (codeBlockLanguage.isEmpty()) {
                        codeBlockLanguage = "text";
                    }
                    
                    // 生成一致的代码块ID（基于在文档中的位置）
                    codeBlockIndex++;
                    currentCodeBlockId = "codeblock-" + codeBlockIndex;
                    
                    // 如果是新代码块，初始化为展开状态
                    if (!codeBlockFoldStates.containsKey(currentCodeBlockId)) {
                        codeBlockFoldStates.put(currentCodeBlockId, false); // 默认展开
                    }
                    
                    boolean isCollapsed = codeBlockFoldStates.getOrDefault(currentCodeBlockId, false);
                    
                    System.out.println("📝 生成代码块: " + currentCodeBlockId + ", 语言: " + codeBlockLanguage + ", 折叠: " + isCollapsed + ", 文档位置: " + codeBlockIndex);
                    
                    // 创建有边框的代码块结构
                    html.append("<div style=\"border: 1px solid #404040; margin: 6px 0; background: transparent;\">");
                    html.append("<p style=\"margin: 0; padding: 4px 8px; background: transparent; border-bottom: 1px solid #404040;\">");
                    html.append("<a href=\"fold://").append(currentCodeBlockId).append("\" style=\"color: #4FC3F7; text-decoration: none; font-weight: bold; background: transparent;\">");
                    html.append(isCollapsed ? "▶ 展开" : "▼ 折叠");
                    html.append("</a>");
                    html.append(" <span style=\"color: #CCCCCC; font-size: 10px; background: transparent;\">").append(codeBlockLanguage.toUpperCase()).append("</span>");
                    html.append("</p>");
                    
                    // 代码内容容器 - 有内边距但无额外边框
                    if (!isCollapsed) {
                        html.append("<pre style=\"color: #D4D4D4; font-family: monospace; font-size: 10px; padding: 8px; margin: 0; border: none; background: transparent;\"><code>");
                    }
                    inCodeBlock = true;
                }
                continue;
            }
            
            // 表格处理 - 在代码块处理之后，其他处理之前
            if (!inCodeBlock && isTableRow(line)) {
                // 检查下一行是否是分隔符（表头标识）
                if (i + 1 < lines.length && isTableSeparatorRow(lines[i + 1].trim())) {
                    if (!inTable) {
                        closeAllLists(html, listStack);
                        lastListLevel = -1;
                        html.append("<table>");
                        inTable = true;
                    }
                    // 处理表头
                    html.append("<thead><tr>");
                    String[] headers = parseTableRow(line);
                    for (String header : headers) {
                        String trimmed = processInlineFormatting(header.trim());
                        html.append("<th>").append(trimmed).append("</th>");
                    }
                    html.append("</tr></thead><tbody>");
                    i++; // 跳过分隔符行
                    continue;
                } else if (inTable && isTableRow(line)) {
                    // 处理表格数据行
                    html.append("<tr>");
                    String[] cells = parseTableRow(line);
                    for (String cell : cells) {
                        String trimmed = processInlineFormatting(cell.trim());
                        html.append("<td>").append(trimmed).append("</td>");
                    }
                    html.append("</tr>");
                    continue;
                } else if (inTable) {
                    // 非表格行，结束表格
                    html.append("</tbody></table>");
                    inTable = false;
                }
            } else if (inTable && !isTableRow(line)) {
                // 结束表格
                html.append("</tbody></table>");
                inTable = false;
            }
            
            if (inCodeBlock) {
                if (line.startsWith("```")) {
                    // 结束代码块
                    boolean isCollapsed = codeBlockFoldStates.getOrDefault(currentCodeBlockId, false);
                    
                    if (!isCollapsed) {
                        html.append("</code></pre>");
                    }
                    // 关闭代码块容器div
                    html.append("</div>");
                    
                    System.out.println("📝 结束代码块: " + currentCodeBlockId);
                    inCodeBlock = false;
                    codeBlockLanguage = null;
                    currentCodeBlockId = null;
                } else {
                    // 代码内容 - 只有在非折叠状态下才添加
                    boolean isCollapsed = codeBlockFoldStates.getOrDefault(currentCodeBlockId, false);
                    
                    if (!isCollapsed) {
                        String highlightedCode = applySyntaxHighlighting(line, codeBlockLanguage);
                        html.append(highlightedCode).append("\n");
                    }
                }
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
            html.append("</code></pre>");
            if (currentCodeBlockId != null) {
                html.append("</div></div>\n");
            } else {
                html.append("\n");
            }
        }
        
        // 关闭未闭合的表格
        if (inTable) {
            html.append("</tbody></table>");
        }
        
        closeAllLists(html, listStack);
        
        return html.toString();
    }
    
    /**
     * 处理行内格式（粗体、斜体、链接等） - 性能优化版本
     */
    private String processInlineFormatting(String text) {
        if (text == null) return "";
        
        // 先进行HTML转义
        text = escapeHtml(text);
        
        // 使用预编译的正则表达式进行格式化，避免重复编译
        
        // 处理链接 [text](url)
        text = LINK_PATTERN.matcher(text).replaceAll("<a href=\"$2\">$1</a>");
        
        // 处理粗体 **text**
        text = BOLD_PATTERN.matcher(text).replaceAll("<strong>$1</strong>");
        
        // 处理斜体 *text*  
        text = ITALIC_PATTERN.matcher(text).replaceAll("<em>$1</em>");
        
        // 处理行内代码 `code`
        text = INLINE_CODE_PATTERN.matcher(text).replaceAll("<code>$1</code>");
        
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
     * 简化的Java语法高亮 (直接方式，避免临时标记)
     */
    private String highlightJavaSimple(String line) {
        if (line == null || line.trim().isEmpty()) {
            return escapeHtml(line);
        }
        
        // 先转义HTML特殊字符
        line = escapeHtml(line);
        
        // 直接进行替换，按优先级顺序
        
        // 1. 处理行注释 (优先级最高) - 使用预编译模式和字符串常量
        if (line.contains("//")) {
            int commentIndex = line.indexOf("//");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            return processJavaCode(beforeComment) + "<span style=\"" + COMMENT_STYLE + "\">" + comment + "</span>";
        }
        
        // 2. 处理多行注释 - 使用字符串常量
        line = line.replaceAll("/\\*([^*]*)\\*/", "<span style=\"" + COMMENT_STYLE + "\">/*$1*/</span>");
        
        // 3. 处理字符串 (在关键字之前) - 使用字符串常量
        line = line.replaceAll("\"([^\"]*)\"", "<span style=\"" + STRING_STYLE + "\">\"$1\"</span>");
        line = line.replaceAll("'([^']*)'", "<span style=\"" + STRING_STYLE + "\">'$1'</span>");
        
        // 4. 处理剩余的代码部分
        return processJavaCode(line);
    }
    
    /**
     * 处理Java代码的关键字和数字高亮 (性能优化版本)
     */
    private String processJavaCode(String line) {
        // 处理数字 - 使用字符串常量
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span style=\"" + NUMBER_STYLE + "\">$1</span>");
        
        // 处理关键字 - 使用字符串常量，避免重复字符串拼接
        String[] javaKeywords = {"public", "private", "protected", "static", "final", "abstract", 
                                "class", "interface", "extends", "implements", "import", "package",
                                "if", "else", "for", "while", "do", "switch", "case", "default",
                                "break", "continue", "return", "try", "catch", "finally", "throw", "throws",
                                "new", "this", "super", "null", "true", "false", "void", "int", "String",
                                "boolean", "double", "float", "long", "char", "byte", "short"};
        
        // 预构建样式字符串以避免重复拼接
        String keywordPrefix = "<span style=\"" + KEYWORD_STYLE + "\">";
        String keywordSuffix = "</span>";
        
        for (String keyword : javaKeywords) {
            // 只替换完整的单词，避免替换HTML标签中的内容
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", keywordPrefix + keyword + keywordSuffix);
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
        
        // 处理行注释 - 使用内联样式
        if (line.contains("//")) {
            int commentIndex = line.indexOf("//");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            return processJSCode(beforeComment) + "<span style=\"color: #6A9955; font-style: italic;\">" + comment + "</span>";
        }
        
        // 处理多行注释 - 使用内联样式
        line = line.replaceAll("/\\*([^*]*)\\*/", "<span style=\"color: #6A9955; font-style: italic;\">/*$1*/</span>");
        
        // 处理字符串 - 使用内联样式
        line = line.replaceAll("\"([^\"]*)\"", "<span style=\"color: #CE9178;\">\"$1\"</span>");
        line = line.replaceAll("'([^']*)'", "<span style=\"color: #CE9178;\">'$1'</span>");
        line = line.replaceAll("`([^`]*)`", "<span style=\"color: #CE9178;\">`$1`</span>"); // 模板字符串
        
        return processJSCode(line);
    }
    
    /**
     * 处理JavaScript代码的关键字和数字高亮 (使用内联样式)
     */
    private String processJSCode(String line) {
        // 处理数字 - 使用内联样式
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span style=\"color: #B5CEA8;\">$1</span>");
        
        // 处理关键字 - 使用内联样式
        String[] jsKeywords = {"function", "var", "let", "const", "if", "else", "for", "while", 
                              "do", "switch", "case", "default", "break", "continue", "return",
                              "try", "catch", "finally", "throw", "new", "this", "typeof", "instanceof",
                              "true", "false", "null", "undefined", "class", "extends"};
        
        for (String keyword : jsKeywords) {
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", "<span style=\"color: #569CD6; font-weight: bold;\">" + keyword + "</span>");
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
        
        // 处理Python注释 - 使用内联样式
        if (line.contains("#")) {
            int commentIndex = line.indexOf("#");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            return processPythonCode(beforeComment) + "<span style=\"color: #6A9955; font-style: italic;\">" + comment + "</span>";
        }
        
        // 处理字符串 - 使用内联样式
        line = line.replaceAll("\"([^\"]*)\"", "<span style=\"color: #CE9178;\">\"$1\"</span>");
        line = line.replaceAll("'([^']*)'", "<span style=\"color: #CE9178;\">'$1'</span>");
        
        return processPythonCode(line);
    }
    
    /**
     * 处理Python代码的关键字和数字高亮 (使用内联样式)
     */
    private String processPythonCode(String line) {
        // 处理数字 - 使用内联样式
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span style=\"color: #B5CEA8;\">$1</span>");
        
        // 处理关键字 - 使用内联样式
        String[] pythonKeywords = {"def", "class", "if", "elif", "else", "for", "while", "break", 
                                  "continue", "return", "try", "except", "finally", "raise",
                                  "import", "from", "as", "with", "pass", "lambda", "yield",
                                  "True", "False", "None", "and", "or", "not", "in", "is"};
        
        for (String keyword : pythonKeywords) {
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", "<span style=\"color: #569CD6; font-weight: bold;\">" + keyword + "</span>");
        }
        
        return line;
    }
    
    /**
     * 简化的HTML语法高亮 (使用内联样式)
     */
    private String highlightHtmlSimple(String line) {
        line = escapeHtml(line);
        
        // HTML标签 (已转义的) - 使用内联样式
        line = line.replaceAll("&lt;(/?)([a-zA-Z][a-zA-Z0-9]*)(.*?)&gt;", 
                              "<span style=\"color: #569CD6; font-weight: bold;\">&lt;$1$2</span><span style=\"color: #9CDCFE;\">$3</span><span style=\"color: #569CD6; font-weight: bold;\">&gt;</span>");
        
        // HTML属性 - 使用内联样式
        line = line.replaceAll("([a-zA-Z-]+)=&quot;([^&]*)&quot;", 
                              "<span style=\"color: #DCDCAA;\">$1</span>=<span style=\"color: #CE9178;\">&quot;$2&quot;</span>");
        
        return line;
    }
    
    /**
     * 简化的CSS语法高亮 (使用内联样式)
     */
    private String highlightCssSimple(String line) {
        line = escapeHtml(line);
        
        // CSS选择器 - 使用内联样式
        line = line.replaceAll("^([.#]?[a-zA-Z][a-zA-Z0-9_-]*)", "<span style=\"color: #4EC9B0;\">$1</span>");
        
        // CSS属性 - 使用内联样式
        line = line.replaceAll("([a-zA-Z-]+):", "<span style=\"color: #DCDCAA;\">$1</span>:");
        
        // CSS值 - 使用内联样式
        line = line.replaceAll(":([^;]+);", ": <span style=\"color: #CE9178;\">$1</span>;");
        
        return line;
    }
    
    /**
     * 简化的JSON语法高亮 (使用内联样式)
     */
    private String highlightJsonSimple(String line) {
        line = escapeHtml(line);
        
        // JSON键 - 使用内联样式
        line = line.replaceAll("&quot;([^&]+?)&quot;:", "<span style=\"color: #DCDCAA;\">&quot;$1&quot;</span>:");
        
        // JSON字符串值 - 使用内联样式
        line = line.replaceAll(":&quot;([^&]*)&quot;", ": <span style=\"color: #CE9178;\">&quot;$1&quot;</span>");
        
        // JSON数字、布尔值、null - 使用内联样式
        line = line.replaceAll("\\b(true|false|null)\\b", "<span style=\"color: #569CD6; font-weight: bold;\">$1</span>");
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span style=\"color: #B5CEA8;\">$1</span>");
        
        return line;
    }
    
    /**
     * 简化的SQL语法高亮 (使用内联样式)
     */
    private String highlightSqlSimple(String line) {
        line = escapeHtml(line);
        
        // 处理字符串 - 使用内联样式
        line = line.replaceAll("'([^']*)'", "<span style=\"color: #CE9178;\">'$1'</span>");
        
        // 处理数字 - 使用内联样式
        line = line.replaceAll("\\b(\\d+\\.?\\d*)\\b", "<span style=\"color: #B5CEA8;\">$1</span>");
        
        // 处理关键字 - 使用内联样式
        String[] sqlKeywords = {"SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", 
                               "TABLE", "INDEX", "DROP", "ALTER", "JOIN", "INNER", "LEFT", "RIGHT",
                               "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION",
                               "AND", "OR", "NOT", "NULL", "TRUE", "FALSE", "AS", "DISTINCT"};
        
        for (String keyword : sqlKeywords) {
            line = line.replaceAll("\\b" + keyword.toLowerCase() + "\\b(?![^<]*>)", "<span style=\"color: #569CD6; font-weight: bold;\">" + keyword.toLowerCase() + "</span>");
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*>)", "<span style=\"color: #569CD6; font-weight: bold;\">" + keyword + "</span>");
        }
        
        return line;
    }
    
    /**
     * 包装HTML文档
     */
    private String wrapHtmlDocument(String bodyContent) {
        String css = getDarkThemeCss();
        
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
            
            // 改进的表格检测：更严格的Markdown表格格式检测
            if (isTableRow(line) && !line.isEmpty()) {
                // 检查下一行是否是分隔符（更严格的检测）
                if (i + 1 < lines.length && isTableSeparatorRow(lines[i + 1].trim())) {
                    if (!inTable) {
                        result.append("<table>\n<thead>\n");
                        inTable = true;
                    }
                    // 处理表头
                    result.append("<tr>");
                    String[] headers = parseTableRow(line);
                    for (String header : headers) {
                        String trimmed = processInlineFormatting(header.trim());
                        result.append("<th>").append(trimmed).append("</th>");
                    }
                    result.append("</tr>\n</thead>\n<tbody>\n");
                    i++; // 跳过分隔符行
                } else if (inTable && isTableRow(line)) {
                    // 处理表格数据行
                    result.append("<tr>");
                    String[] cells = parseTableRow(line);
                    for (String cell : cells) {
                        String trimmed = processInlineFormatting(cell.trim());
                        result.append("<td>").append(trimmed).append("</td>");
                    }
                    result.append("</tr>\n");
                } else {
                    if (inTable) {
                        result.append("</tbody>\n</table>\n");
                        inTable = false;
                    }
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
    
    /**
     * 检查是否是表格行
     */
    private boolean isTableRow(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // 标准Markdown表格格式：
        // - 包含至少一个 | 字符
        // - 可以以 | 开始和结束（可选）
        // - 至少包含两个单元格（即至少一个分隔符 |）
        
        String trimmed = line.trim();
        int pipeCount = 0;
        
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) == '|') {
                pipeCount++;
            }
        }
        
        // 表格行至少需要包含一个管道符，且管道符数量合理
        return pipeCount >= 1 && pipeCount <= 20; // 合理的管道符数量限制
    }
    
    /**
     * 检查是否是表格分隔符行
     */
    private boolean isTableSeparatorRow(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = line.trim();
        
        // 标准Markdown表格分隔符格式：
        // - 包含 | 和 - 字符
        // - 可以包含 : 用于对齐
        // - 可以有空格
        
        // 移除首尾的 | 字符
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        
        // 分割并检查每个单元格
        String[] cells = trimmed.split("\\|");
        
        for (String cell : cells) {
            String cellTrimmed = cell.trim();
            if (cellTrimmed.isEmpty()) {
                continue;
            }
            
            // 每个单元格应该只包含 -、: 和空格
            if (!cellTrimmed.matches("^[:\\s-]+$")) {
                return false;
            }
            
            // 至少要有一个 - 字符
            if (!cellTrimmed.contains("-")) {
                return false;
            }
        }
        
        return cells.length > 0;
    }
    
    /**
     * 解析表格行，正确处理首尾的管道符
     */
    private String[] parseTableRow(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new String[0];
        }
        
        String trimmed = line.trim();
        
        // 移除首尾的 | 字符（如果存在）
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        
        // 分割单元格
        String[] cells = trimmed.split("\\|", -1); // -1 保留空字符串
        
        // 过滤掉完全空白的单元格（但保留有意义的空格）
        java.util.List<String> nonEmptyCells = new java.util.ArrayList<>();
        for (String cell : cells) {
            nonEmptyCells.add(cell); // 保留所有单元格，包括空的
        }
        
        return nonEmptyCells.toArray(new String[0]);
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
            String highlightedLine = applySyntaxHighlighting(line, language);
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
    private String applySyntaxHighlighting(String line, String language) {
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
        
        // 处理剩余内容 - 先不转义，等语法高亮完成后再处理
        String remaining = line.substring(i);
        processedLine.append(remaining);
        
        // 先应用语法高亮（此时还是原始文本）
        String highlighted = highlightByLanguage(processedLine.toString(), language);
        
        // 最后对非HTML标签部分进行HTML转义
        return finalEscapeHtml(highlighted);
    }
    
    /**
     * 智能HTML转义：只转义非HTML标签部分的特殊字符
     */
    private String finalEscapeHtml(String highlighted) {
        if (highlighted == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < highlighted.length()) {
            // 检查是否是HTML标签开始
            if (highlighted.charAt(i) == '<') {
                int tagEnd = highlighted.indexOf('>', i);
                if (tagEnd != -1) {
                    // 这是一个完整的HTML标签，直接保留
                    result.append(highlighted.substring(i, tagEnd + 1));
                    i = tagEnd + 1;
                } else {
                    // 不是完整标签，转义这个 <
                    result.append("&lt;");
                    i++;
                }
            } else if (highlighted.charAt(i) == '&') {
                // 检查是否已经是转义序列
                int semicolon = highlighted.indexOf(';', i);
                if (semicolon != -1 && semicolon - i <= 8) { // 常见转义序列长度不超过8
                    String sequence = highlighted.substring(i, semicolon + 1);
                    if (sequence.matches("&(nbsp|amp|lt|gt|quot|#39|#x[0-9a-fA-F]+|#\\d+);")) {
                        // 已经是转义序列，保留
                        result.append(sequence);
                        i = semicolon + 1;
                    } else {
                        // 普通的&符号，转义
                        result.append("&amp;");
                        i++;
                    }
                } else {
                    // 普通的&符号，转义
                    result.append("&amp;");
                    i++;
                }
            } else {
                // 其他字符检查是否需要转义
                char c = highlighted.charAt(i);
                switch (c) {
                    case '>':
                        result.append("&gt;");
                        break;
                    case '"':
                        result.append("&quot;");
                        break;
                    case '\'':
                        result.append("&#39;");
                        break;
                    default:
                        result.append(c);
                        break;
                }
                i++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * 根据语言应用语法高亮
     */
    private String highlightByLanguage(String line, String language) {
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
        
        String keywordColor = isDark ? "#ff7b72" : "#cf222e";  // 红色
        String stringColor = isDark ? "#a5d6ff" : "#0a3069";   // 蓝色
        String commentColor = isDark ? "#8b949e" : "#6a737d";  // 灰色
        
        // 处理注释（优先级最高，避免在注释中进行其他高亮）
        if (line.contains("//")) {
            int commentIndex = line.indexOf("//");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            
            // 对注释前的部分进行高亮处理
            beforeComment = highlightJavaKeywordsAndStrings(beforeComment, keywords, keywordColor, stringColor);
            
            // 注释部分单独处理
            comment = "<span style=\"color: " + commentColor + "; font-style: italic; background: transparent;\">" + comment + "</span>";
            
            return beforeComment + comment;
        } else {
            // 没有注释，正常处理
            return highlightJavaKeywordsAndStrings(line, keywords, keywordColor, stringColor);
        }
    }
    
    /**
     * 高亮Java关键字和字符串
     */
    private String highlightJavaKeywordsAndStrings(String line, String[] keywords, String keywordColor, String stringColor) {
        // 先处理字符串（避免在字符串中高亮关键字）
        line = line.replaceAll("\"([^\"]*?)\"", 
            "<span style=\"color: " + stringColor + "; background: transparent;\">\"$1\"</span>");
        
        // 然后处理关键字（避免在已高亮的内容中再次高亮）
        for (String keyword : keywords) {
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*</span>)", 
                "<span style=\"color: " + keywordColor + "; font-weight: bold; background: transparent;\">" + keyword + "</span>");
        }
        
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
        
        // 先处理字符串（避免在字符串中高亮关键字）
        line = line.replaceAll("\"([^\"]*?)\"", 
            "<span style=\"color: " + stringColor + "; background: transparent;\">\"$1\"</span>");
        line = line.replaceAll("'([^']*?)'", 
            "<span style=\"color: " + stringColor + "; background: transparent;\">'$1'</span>");
        
        // 然后处理关键字（避免在已高亮的内容中再次高亮）
        for (String keyword : keywords) {
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*</span>)", 
                "<span style=\"color: " + keywordColor + "; font-weight: bold; background: transparent;\">" + keyword + "</span>");
        }
        
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
        
        // 处理注释（优先级最高）
        if (line.contains("#")) {
            int commentIndex = line.indexOf("#");
            String beforeComment = line.substring(0, commentIndex);
            String comment = line.substring(commentIndex);
            
            // 对注释前的部分进行高亮处理
            beforeComment = highlightPythonKeywordsAndStrings(beforeComment, keywords, keywordColor, stringColor);
            
            // 注释部分单独处理
            comment = "<span style=\"color: " + commentColor + "; font-style: italic; background: transparent;\">" + comment + "</span>";
            
            return beforeComment + comment;
        } else {
            // 没有注释，正常处理
            return highlightPythonKeywordsAndStrings(line, keywords, keywordColor, stringColor);
        }
    }
    
    /**
     * 高亮Python关键字和字符串
     */
    private String highlightPythonKeywordsAndStrings(String line, String[] keywords, String keywordColor, String stringColor) {
        // 先处理字符串（避免在字符串中高亮关键字）
        line = line.replaceAll("\"([^\"]*?)\"", 
            "<span style=\"color: " + stringColor + "; background: transparent;\">\"$1\"</span>");
        line = line.replaceAll("'([^']*?)'", 
            "<span style=\"color: " + stringColor + "; background: transparent;\">'$1'</span>");
        
        // 然后处理关键字（避免在已高亮的内容中再次高亮）
        for (String keyword : keywords) {
            line = line.replaceAll("\\b" + keyword + "\\b(?![^<]*</span>)", 
                "<span style=\"color: " + keywordColor + "; font-weight: bold; background: transparent;\">" + keyword + "</span>");
        }
        
        return line;
    }
    
    /**
     * HTML语法高亮
     */
    private String highlightHtml(String line, boolean isDark) {
        String tagColor = isDark ? "#7ee787" : "#116329";  // 绿色 - GitHub主题使用更深的绿色
        
        // HTML标签高亮
        line = line.replaceAll("&lt;([^&gt;]+)&gt;", 
            "<span style=\"color: " + tagColor + "; font-weight: bold; background: transparent;\">&lt;$1&gt;</span>");
        
        return line;
    }
    
    /**
     * CSS语法高亮
     */
    private String highlightCss(String line, boolean isDark) {
        String propertyColor = isDark ? "#ff7b72" : "#cf222e";
        
        // CSS属性高亮
        line = line.replaceAll("([a-zA-Z-]+):", 
            "<span style=\"color: " + propertyColor + "; background: transparent;\">$1</span>:");
        
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
            "<span style=\"color: " + keyColor + "; font-weight: bold; background: transparent;\">\"$1\"</span>:");
        line = line.replaceAll(":&nbsp;\"([^\"]*?)\"", 
            ": <span style=\"color: " + valueColor + "; background: transparent;\">\"$1\"</span>");
        
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
            "<span style=\"color: " + stringColor + "; background: transparent;\">\"$1\"</span>");
        line = line.replaceAll("'([^']*?)'", 
            "<span style=\"color: " + stringColor + "; background: transparent;\">'$1'</span>");
        
        // 数字高亮
        line = line.replaceAll("\\b(\\d+)\\b", 
            "<span style=\"color: " + numberColor + "; background: transparent;\">$1</span>");
        
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
     * 切换代码块折叠状态
     * @param codeBlockId 代码块ID
     * @return 切换后的状态 (true=折叠, false=展开)
     */
    public boolean toggleCodeBlockFold(String codeBlockId) {
        boolean currentState = codeBlockFoldStates.getOrDefault(codeBlockId, false);
        boolean newState = !currentState;
        codeBlockFoldStates.put(codeBlockId, newState);
        System.out.println("🔀 代码块 " + codeBlockId + " 折叠状态: " + (newState ? "折叠" : "展开"));
        return newState;
    }
    
    /**
     * 重置所有代码块状态
     */
    public void resetCodeBlockStates() {
        // 不需要重置计数器，因为现在基于文档位置
        // 保留用户的折叠偏好，不清空codeBlockFoldStates
        System.out.println("🔄 代码块状态重置（保留折叠偏好）");
    }
    
    /**
     * 获取代码块折叠状态
     * @param codeBlockId 代码块ID
     * @return 折叠状态 (true=折叠, false=展开)
     */
    public boolean isCodeBlockFolded(String codeBlockId) {
        return codeBlockFoldStates.getOrDefault(codeBlockId, false);
    }
    
    /**
     * 测试方法：输出生成的HTML用于调试
     */
    public void debugGeneratedHtml(String markdownText) {
        System.out.println("🔍 调试HTML生成 - 输入Markdown:");
        System.out.println("=====================================");
        System.out.println(markdownText);
        System.out.println("=====================================");
        
        String html = processMarkdown(markdownText);
        
        System.out.println("🔍 生成的HTML:");
        System.out.println("=====================================");
        System.out.println(html);
        System.out.println("=====================================");
        
        System.out.println("🔍 代码块折叠状态:");
        codeBlockFoldStates.forEach((id, state) -> 
            System.out.println("  " + id + ": " + (state ? "折叠" : "展开"))
        );
    }
    
    /**
     * 释放处理器资源 (性能优化版本)
     */
    public void dispose() {
        System.out.println("🗑️ 释放MarkdownProcessor资源");
        
        try {
            // 标记为已释放
            disposed = true;
            
            // 清空主题设置
            
            // 清空代码块状态
            codeBlockFoldStates.clear();
            
            // 注意：parser和flavour是final的，让GC自动回收
            
            System.out.println("✅ MarkdownProcessor资源释放完成");
            
        } catch (Exception e) {
            System.err.println("❌ 释放MarkdownProcessor资源时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
