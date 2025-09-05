package com.markdown.editor.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Markdown编辑器设置服务
 */
@Service
@State(name = "MarkdownEditorSettings", storages = @Storage("markdownEditor.xml"))
public final class MarkdownSettings implements PersistentStateComponent<MarkdownSettings> {
    
    private boolean enablePreview = true;
    private boolean enableSyntaxHighlight = true;
    private boolean enableCodeFolding = true;
    private boolean enableAutoSave = false;
    private int autoSaveInterval = 30;
    
    public static MarkdownSettings getInstance() {
        return ApplicationManager.getApplication().getService(MarkdownSettings.class);
    }

    @Nullable
    @Override
    public MarkdownSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MarkdownSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getters and Setters
    public boolean isEnablePreview() {
        return enablePreview;
    }

    public void setEnablePreview(boolean enablePreview) {
        this.enablePreview = enablePreview;
    }

    public boolean isEnableSyntaxHighlight() {
        return enableSyntaxHighlight;
    }

    public void setEnableSyntaxHighlight(boolean enableSyntaxHighlight) {
        this.enableSyntaxHighlight = enableSyntaxHighlight;
    }

    public boolean isEnableCodeFolding() {
        return enableCodeFolding;
    }

    public void setEnableCodeFolding(boolean enableCodeFolding) {
        this.enableCodeFolding = enableCodeFolding;
    }

    public boolean isEnableAutoSave() {
        return enableAutoSave;
    }

    public void setEnableAutoSave(boolean enableAutoSave) {
        this.enableAutoSave = enableAutoSave;
    }


    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public void setAutoSaveInterval(int autoSaveInterval) {
        this.autoSaveInterval = autoSaveInterval;
    }
}
