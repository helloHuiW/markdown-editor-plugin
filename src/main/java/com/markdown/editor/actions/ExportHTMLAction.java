package com.markdown.editor.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.markdown.editor.file.MarkdownFileType;
import com.markdown.editor.preview.MarkdownProcessor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 导出HTML文件操作
 */
public class ExportHTMLAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        
        if (project == null || file == null) return;
        
        try {
            // 选择保存位置
            FileSaverDescriptor descriptor = new FileSaverDescriptor("导出HTML", "选择保存位置", "html");
            FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
            
            var fileWrapper = dialog.save(file.getParent(), file.getNameWithoutExtension() + ".html");
            if (fileWrapper == null) return;
            
            String targetPath = fileWrapper.getFile().getAbsolutePath();
            
            // 读取Markdown内容
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) return;
            
            String markdownContent = psiFile.getText();
            
            // 转换为HTML
            MarkdownProcessor processor = new MarkdownProcessor();
            processor.setTheme("GitHub");
            String htmlContent = processor.processMarkdown(markdownContent);
            
            // 保存HTML文件
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    Path path = Paths.get(targetPath);
                    Files.write(path, htmlContent.getBytes(StandardCharsets.UTF_8));
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showInfoMessage(project, 
                            "HTML文件已成功导出到:\n" + targetPath, 
                            "导出成功");
                    });
                } catch (IOException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, 
                            "导出失败: " + ex.getMessage(), 
                            "导出错误");
                    });
                }
            });
            
        } catch (Exception ex) {
            Messages.showErrorDialog(project, 
                "导出失败: " + ex.getMessage(), 
                "导出错误");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        
        boolean enabled = project != null && 
                         file != null && 
                         (file.getFileType() instanceof MarkdownFileType ||
                          isMarkdownFile(file));
        
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
    }
    
    private boolean isMarkdownFile(VirtualFile file) {
        String extension = file.getExtension();
        return extension != null && 
               (extension.equals("md") || 
                extension.equals("markdown") || 
                extension.equals("mdown") || 
                extension.equals("mkd"));
    }
}
