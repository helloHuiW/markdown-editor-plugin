package com.markdown.editor.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.intellij.plugins.markdown.lang.MarkdownElementTypes;

/**
 * Markdown代码折叠构建器
 * 为Markdown文档提供代码折叠功能
 */
public class MarkdownFoldingBuilder implements FoldingBuilder {

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        collectFoldingRegions(node, descriptors, document);
        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void collectFoldingRegions(ASTNode node, List<FoldingDescriptor> descriptors, Document document) {
        if (isFoldableElement(node)) {
            TextRange range = node.getTextRange();
            if (range.getLength() > 1) {
                String placeholder = getPlaceholderText(node);
                descriptors.add(new FoldingDescriptor(node, range, null, placeholder));
            }
        }

        // 递归处理子节点
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            collectFoldingRegions(child, descriptors, document);
        }
    }

    private boolean isFoldableElement(ASTNode node) {
        return node.getElementType() == MarkdownElementTypes.CODE_BLOCK ||
               node.getElementType() == MarkdownElementTypes.CODE_FENCE ||
               node.getElementType() == MarkdownElementTypes.BLOCK_QUOTE ||
               node.getElementType() == MarkdownElementTypes.TABLE ||
               isHeader(node);
    }

    private boolean isHeader(ASTNode node) {
        return node.getElementType() == MarkdownElementTypes.ATX_1 ||
               node.getElementType() == MarkdownElementTypes.ATX_2 ||
               node.getElementType() == MarkdownElementTypes.ATX_3 ||
               node.getElementType() == MarkdownElementTypes.ATX_4 ||
               node.getElementType() == MarkdownElementTypes.ATX_5 ||
               node.getElementType() == MarkdownElementTypes.ATX_6 ||
               node.getElementType() == MarkdownElementTypes.SETEXT_1 ||
               node.getElementType() == MarkdownElementTypes.SETEXT_2;
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        if (node.getElementType() == MarkdownElementTypes.CODE_BLOCK || node.getElementType() == MarkdownElementTypes.CODE_FENCE) {
            return "{ 代码块 }";
        } else if (node.getElementType() == MarkdownElementTypes.BLOCK_QUOTE) {
            return "{ 引用块 }";
        } else if (node.getElementType() == MarkdownElementTypes.TABLE) {
            return "{ 表格 }";
        } else if (isHeader(node)) {
            String text = node.getText();
            if (text.length() > 50) {
                return text.substring(0, 47) + "...";
            }
            return text;
        }
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        // 默认不折叠任何元素
        return false;
    }
}
