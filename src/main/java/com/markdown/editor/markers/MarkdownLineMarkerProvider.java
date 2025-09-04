package com.markdown.editor.markers;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * Markdown行标记提供者
 * 为Markdown元素提供行号旁的图标标记
 */
public class MarkdownLineMarkerProvider implements LineMarkerProvider {
    
    // 使用内置图标，避免图标文件缺失问题
    private static final Icon HEADER_ICON = null; // IconLoader.getIcon("/icons/header.svg", MarkdownLineMarkerProvider.class);
    private static final Icon LINK_ICON = null; // IconLoader.getIcon("/icons/link.svg", MarkdownLineMarkerProvider.class);
    private static final Icon IMAGE_ICON = null; // IconLoader.getIcon("/icons/image.svg", MarkdownLineMarkerProvider.class);
    private static final Icon CODE_ICON = null; // IconLoader.getIcon("/icons/code.svg", MarkdownLineMarkerProvider.class);

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // 暂时禁用行标记功能，避免图标加载问题
        return null;
    }
    
    private LineMarkerInfo<PsiElement> createLineMarkerInfo(PsiElement element, Icon icon, String tooltip) {
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            icon,
            e -> tooltip,
            null,
            GutterIconRenderer.Alignment.LEFT,
            () -> tooltip
        );
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        // 可以在这里进行更复杂的行标记收集
        for (PsiElement element : elements) {
            LineMarkerInfo<?> markerInfo = getLineMarkerInfo(element);
            if (markerInfo != null) {
                result.add(markerInfo);
            }
        }
    }
}
