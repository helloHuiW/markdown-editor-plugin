package com.markdown.editor.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.markdown.editor.file.MarkdownFileType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * 简化的新建Markdown文件操作
 */
public class SimpleNewMarkdownFileAction extends AnAction {
    
    public SimpleNewMarkdownFileAction() {
        super("Markdown文件", "创建一个新的Markdown文件", MarkdownFileType.INSTANCE.getIcon());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        VirtualFile directory = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (directory == null || !directory.isDirectory()) {
            directory = project.getBaseDir();
        }
        
        if (directory == null) return;
        
        // 询问文件名
        String fileName = Messages.showInputDialog(
            project,
            "请输入Markdown文件名:",
            "新建Markdown文件",
            Messages.getQuestionIcon(),
            "untitled",
            new InputValidator() {
                @Override
                public boolean checkInput(String inputString) {
                    return inputString != null && !inputString.trim().isEmpty();
                }
                
                @Override
                public boolean canClose(String inputString) {
                    return checkInput(inputString);
                }
            }
        );
        
        if (fileName == null || fileName.trim().isEmpty()) return;
        
        // 确保文件扩展名
        if (!fileName.endsWith(".md")) {
            fileName += ".md";
        }
        
        final String finalFileName = fileName;
        final VirtualFile finalDirectory = directory;
        
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // 创建文件
                VirtualFile newFile = finalDirectory.createChildData(this, finalFileName);
                
                // 写入默认内容
                String defaultContent = String.format(
                    "# %s\n\n这是一个新的Markdown文件。\n\n## 开始编写\n\n请在这里添加您的内容...\n",
                    finalFileName.replace(".md", "")
                );
                newFile.setBinaryContent(defaultContent.getBytes());
                
                // 打开文件
                PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(finalDirectory);
                if (psiDirectory != null) {
                    com.intellij.ide.util.EditorHelper.openInEditor(
                        PsiManager.getInstance(project).findFile(newFile)
                    );
                }
                
            } catch (IOException ex) {
                Messages.showErrorDialog(project, "创建文件失败: " + ex.getMessage(), "错误");
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}
