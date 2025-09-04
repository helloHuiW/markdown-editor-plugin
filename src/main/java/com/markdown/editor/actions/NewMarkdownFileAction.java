package com.markdown.editor.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.markdown.editor.file.MarkdownFileType;
import org.jetbrains.annotations.NotNull;

/**
 * 新建Markdown文件操作
 */
public class NewMarkdownFileAction extends CreateFileFromTemplateAction {
    
    private static final String ACTION_NAME = "新建Markdown文件";
    private static final String ACTION_DESCRIPTION = "创建一个新的Markdown文件";
    
    public NewMarkdownFileAction() {
        super(ACTION_NAME, ACTION_DESCRIPTION, MarkdownFileType.INSTANCE.getIcon());
    }

    @Override
    protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory, CreateFileFromTemplateDialog.@NotNull Builder builder) {
        builder.setTitle("新建Markdown文件")
               .addKind("空白文件", MarkdownFileType.INSTANCE.getIcon(), "markdown_empty")
               .addKind("README文件", MarkdownFileType.INSTANCE.getIcon(), "markdown_readme")
               .addKind("文档模板", MarkdownFileType.INSTANCE.getIcon(), "markdown_doc");
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return ACTION_NAME;
    }

    @Override
    protected PsiFile createFile(String name, String templateName, PsiDirectory dir) {
        String content = getTemplateContent(templateName, name);
        
        PsiFileFactory factory = PsiFileFactory.getInstance(dir.getProject());
        PsiFile file = factory.createFileFromText(name + ".md", MarkdownFileType.INSTANCE, content);
        
        return (PsiFile) dir.add(file);
    }
    
    private String getTemplateContent(String templateName, String fileName) {
        switch (templateName) {
            case "markdown_readme":
                return String.format("# %s\n\n## 简介\n\n这是一个新的项目。\n\n## 安装\n\n```bash\n# 安装说明\n```\n\n## 使用方法\n\n描述如何使用这个项目。\n\n## 贡献\n\n欢迎贡献代码！\n\n## 许可证\n\nMIT License", fileName);
            
            case "markdown_doc":
                return String.format("# %s\n\n## 概述\n\n简要描述文档内容。\n\n## 目录\n\n- [章节1](#章节1)\n- [章节2](#章节2)\n- [章节3](#章节3)\n\n## 章节1\n\n内容...\n\n## 章节2\n\n内容...\n\n## 章节3\n\n内容...\n\n---\n\n*文档创建于: %s*", fileName, java.time.LocalDate.now().toString());
            
            case "markdown_empty":
            default:
                return String.format("# %s\n\n开始编写您的Markdown内容...", fileName);
        }
    }
}
