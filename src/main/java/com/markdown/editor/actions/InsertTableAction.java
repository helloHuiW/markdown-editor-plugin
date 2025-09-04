package com.markdown.editor.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.markdown.editor.file.MarkdownFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 插入表格操作
 */
public class InsertTableAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) return;
        
        TableDialog dialog = new TableDialog(project);
        if (dialog.showAndGet()) {
            int rows = dialog.getRows();
            int cols = dialog.getCols();
            String tableMarkdown = generateTableMarkdown(rows, cols);
            
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Document document = editor.getDocument();
                int offset = editor.getCaretModel().getOffset();
                document.insertString(offset, tableMarkdown);
            });
        }
    }
    
    private String generateTableMarkdown(int rows, int cols) {
        StringBuilder sb = new StringBuilder();
        
        // 表头
        sb.append("|");
        for (int col = 0; col < cols; col++) {
            sb.append(" 列").append(col + 1).append(" |");
        }
        sb.append("\n");
        
        // 分隔行
        sb.append("|");
        for (int col = 0; col < cols; col++) {
            sb.append(" --- |");
        }
        sb.append("\n");
        
        // 数据行
        for (int row = 0; row < rows - 1; row++) {
            sb.append("|");
            for (int col = 0; col < cols; col++) {
                sb.append("  |");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        
        boolean enabled = project != null && 
                         editor != null && 
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
    
    private static class TableDialog extends DialogWrapper {
        private JBTextField rowsField;
        private JBTextField colsField;
        
        protected TableDialog(@Nullable Project project) {
            super(project);
            setTitle("插入表格");
            init();
        }
        
        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            rowsField = new JBTextField("3", 10);
            colsField = new JBTextField("3", 10);
            
            return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("行数:"), rowsField, 1, false)
                .addLabeledComponent(new JBLabel("列数:"), colsField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        }
        
        @Nullable
        @Override
        protected ValidationInfo doValidate() {
            try {
                int rows = Integer.parseInt(rowsField.getText());
                int cols = Integer.parseInt(colsField.getText());
                
                if (rows < 1 || rows > 20) {
                    return new ValidationInfo("行数必须在1-20之间", rowsField);
                }
                if (cols < 1 || cols > 10) {
                    return new ValidationInfo("列数必须在1-10之间", colsField);
                }
            } catch (NumberFormatException e) {
                return new ValidationInfo("请输入有效的数字");
            }
            
            return null;
        }
        
        public int getRows() {
            return Integer.parseInt(rowsField.getText());
        }
        
        public int getCols() {
            return Integer.parseInt(colsField.getText());
        }
    }
}
