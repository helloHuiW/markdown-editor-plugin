package com.markdown.editor.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Markdown工具窗口工厂
 */
public class MarkdownToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MarkdownToolWindow markdownToolWindow = new MarkdownToolWindow(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(markdownToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        
        // 将工具窗口引用传递给MarkdownToolWindow，以便监听状态变化
        markdownToolWindow.setToolWindow(toolWindow);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
