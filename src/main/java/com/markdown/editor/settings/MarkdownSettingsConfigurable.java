package com.markdown.editor.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Markdown编辑器设置面板
 */
public class MarkdownSettingsConfigurable implements Configurable {
    
    private JBCheckBox enablePreviewCheckBox;
    private JBCheckBox enableSyntaxHighlightCheckBox;
    private JBCheckBox enableCodeFoldingCheckBox;
    private JBCheckBox enableAutoSaveCheckBox;
    private JComboBox<String> defaultThemeComboBox;
    private JTextField autoSaveIntervalField;
    
    private final MarkdownSettings settings = MarkdownSettings.getInstance();

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Markdown Editor";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        enablePreviewCheckBox = new JBCheckBox("启用实时预览");
        enableSyntaxHighlightCheckBox = new JBCheckBox("启用语法高亮");
        enableCodeFoldingCheckBox = new JBCheckBox("启用代码折叠");
        enableAutoSaveCheckBox = new JBCheckBox("启用自动保存");
        
        defaultThemeComboBox = new JComboBox<>(new String[]{"GitHub", "暗黑", "简洁"});
        autoSaveIntervalField = new JTextField(10);
        
        return FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("编辑器功能"))
            .addComponent(enablePreviewCheckBox)
            .addComponent(enableSyntaxHighlightCheckBox)
            .addComponent(enableCodeFoldingCheckBox)
            .addSeparator()
            .addComponent(new JBLabel("预览设置"))
            .addLabeledComponent("默认主题:", defaultThemeComboBox)
            .addSeparator()
            .addComponent(new JBLabel("自动保存"))
            .addComponent(enableAutoSaveCheckBox)
            .addLabeledComponent("保存间隔(秒):", autoSaveIntervalField)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    @Override
    public boolean isModified() {
        return enablePreviewCheckBox.isSelected() != settings.isEnablePreview() ||
               enableSyntaxHighlightCheckBox.isSelected() != settings.isEnableSyntaxHighlight() ||
               enableCodeFoldingCheckBox.isSelected() != settings.isEnableCodeFolding() ||
               enableAutoSaveCheckBox.isSelected() != settings.isEnableAutoSave() ||
               !defaultThemeComboBox.getSelectedItem().equals(settings.getDefaultTheme()) ||
               !autoSaveIntervalField.getText().equals(String.valueOf(settings.getAutoSaveInterval()));
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            settings.setEnablePreview(enablePreviewCheckBox.isSelected());
            settings.setEnableSyntaxHighlight(enableSyntaxHighlightCheckBox.isSelected());
            settings.setEnableCodeFolding(enableCodeFoldingCheckBox.isSelected());
            settings.setEnableAutoSave(enableAutoSaveCheckBox.isSelected());
            settings.setDefaultTheme((String) defaultThemeComboBox.getSelectedItem());
            settings.setAutoSaveInterval(Integer.parseInt(autoSaveIntervalField.getText()));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("自动保存间隔必须是一个有效的数字");
        }
    }

    @Override
    public void reset() {
        enablePreviewCheckBox.setSelected(settings.isEnablePreview());
        enableSyntaxHighlightCheckBox.setSelected(settings.isEnableSyntaxHighlight());
        enableCodeFoldingCheckBox.setSelected(settings.isEnableCodeFolding());
        enableAutoSaveCheckBox.setSelected(settings.isEnableAutoSave());
        defaultThemeComboBox.setSelectedItem(settings.getDefaultTheme());
        autoSaveIntervalField.setText(String.valueOf(settings.getAutoSaveInterval()));
    }
}
