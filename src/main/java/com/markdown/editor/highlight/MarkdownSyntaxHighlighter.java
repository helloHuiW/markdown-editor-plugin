package com.markdown.editor.highlight;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.intellij.plugins.markdown.highlighting.MarkdownColorSettingsPage;
import org.intellij.plugins.markdown.lang.lexer.MarkdownLexerAdapter;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;
import org.intellij.plugins.markdown.lang.MarkdownElementTypes;

/**
 * Markdown语法高亮器
 * 定义了Markdown各种语法元素的高亮样式
 */
public class MarkdownSyntaxHighlighter extends SyntaxHighlighterBase {
    
    // 定义高亮颜色键
    public static final TextAttributesKey HEADER = createTextAttributesKey("MARKDOWN_HEADER", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey BOLD = createTextAttributesKey("MARKDOWN_BOLD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey ITALIC = createTextAttributesKey("MARKDOWN_ITALIC", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey CODE_SPAN = createTextAttributesKey("MARKDOWN_CODE_SPAN", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey CODE_BLOCK = createTextAttributesKey("MARKDOWN_CODE_BLOCK", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey LINK = createTextAttributesKey("MARKDOWN_LINK", DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    public static final TextAttributesKey QUOTE = createTextAttributesKey("MARKDOWN_QUOTE", DefaultLanguageHighlighterColors.DOC_COMMENT);
    public static final TextAttributesKey LIST_MARKER = createTextAttributesKey("MARKDOWN_LIST_MARKER", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey TABLE = createTextAttributesKey("MARKDOWN_TABLE", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey STRIKETHROUGH = createTextAttributesKey("MARKDOWN_STRIKETHROUGH", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new MarkdownLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType == MarkdownElementTypes.ATX_1 || tokenType == MarkdownElementTypes.ATX_2 || 
            tokenType == MarkdownElementTypes.ATX_3 || tokenType == MarkdownElementTypes.ATX_4 || 
            tokenType == MarkdownElementTypes.ATX_5 || tokenType == MarkdownElementTypes.ATX_6 ||
            tokenType == MarkdownElementTypes.SETEXT_1 || tokenType == MarkdownElementTypes.SETEXT_2) {
            return new TextAttributesKey[]{HEADER};
        }
        
        if (tokenType == MarkdownElementTypes.EMPH) {
            return new TextAttributesKey[]{ITALIC};
        }
        
        if (tokenType == MarkdownElementTypes.STRONG) {
            return new TextAttributesKey[]{BOLD};
        }
        
        if (tokenType == MarkdownElementTypes.CODE_SPAN) {
            return new TextAttributesKey[]{CODE_SPAN};
        }
        
        if (tokenType == MarkdownElementTypes.CODE_BLOCK || tokenType == MarkdownElementTypes.CODE_FENCE) {
            return new TextAttributesKey[]{CODE_BLOCK};
        }
        
        if (tokenType == MarkdownElementTypes.AUTOLINK || tokenType == MarkdownElementTypes.INLINE_LINK) {
            return new TextAttributesKey[]{LINK};
        }
        
        if (tokenType == MarkdownElementTypes.BLOCK_QUOTE) {
            return new TextAttributesKey[]{QUOTE};
        }
        
        // LIST_MARKER可能不存在于所有版本中，跳过此检查
        // if (tokenType == MarkdownElementTypes.LIST_MARKER) {
        //     return new TextAttributesKey[]{LIST_MARKER};
        // }
        
        if (tokenType == MarkdownElementTypes.TABLE) {
            return new TextAttributesKey[]{TABLE};
        }
        
        return new TextAttributesKey[0];
    }
}
