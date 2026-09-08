package com.smis.security.richtext;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants.ConfigType;
import com.wontlost.ckeditor.Constants.EditorType;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import elemental.json.Json;
import elemental.json.JsonArray;
import java.util.LinkedHashSet;
import java.util.Set;

/** Central presentation boundary; the underlying untrusted editor is never exposed to callers. */
public final class SafeRichTextEditor extends Composite<Div> implements com.vaadin.flow.component.HasSize {
    private final VaadinCKEditor editor;

    public SafeRichTextEditor() {
        Config config = new Config();
        Set<String> removed = new LinkedHashSet<>();
        JsonArray defaults = (JsonArray) config.getConfigs().get(ConfigType.removePlugins);
        for (int i = 0; i < defaults.length(); i++) removed.add(defaults.getString(i));
        removed.addAll(Set.of("SourceEditing", "HtmlEmbed", "GeneralHtmlSupport", "MediaEmbed",
                "MediaEmbedToolbar", "ImageUpload", "ImageInsert", "ImageInsertViaUrl", "AutoImage",
                "Base64UploadAdapter", "SimpleUploadAdapter", "CKFinderUploadAdapter", "CKFinder", "EasyImage",
                "ImageResize", "TableProperties", "TableCellProperties", "TodoList", "PageBreak", "IndentBlock"));
        JsonArray plugins = Json.createArray();
        int index = 0;
        for (String plugin : removed) plugins.set(index++, plugin);
        config.getConfigs().put(ConfigType.removePlugins, plugins);
        JsonArray toolbar = Json.instance().parse("""
                ["heading","fontFamily","fontSize","fontColor","fontBackgroundColor","|",
                "bold","italic","underline","strikethrough","subscript","superscript","removeFormat","|",
                "link","bulletedList","numberedList","alignment","blockQuote","code","codeBlock",
                "insertTable","horizontalLine","specialCharacters","|","undo","redo"]
                """);
        config.getConfigs().put(ConfigType.toolbar, toolbar);
        config.setFontFamily(false, new String[] {"default", "Times New Roman, Times, serif", "Arial, Helvetica, sans-serif"});
        config.setFontSize(false, new String[] {"10", "12", "14", "16", "18", "24"});
        editor = new VaadinCKEditorBuilder().with(builder -> {
            builder.editorData = "";
            builder.editorType = EditorType.INLINE;
            builder.config = config;
            builder.ghsEnabled = false;
            builder.autosave = false;
        }).createVaadinCKEditor();
        editor.setSizeFull();
        getContent().add(editor);
    }

    public void setValue(String untrustedHtml) {
        editor.setValue(RichTextHtmlSanitizer.INSTANCE.sanitizeForRendering(untrustedHtml));
    }

    public String getValue() {
        return RichTextHtmlSanitizer.INSTANCE.sanitizeForStorage(editor.getValue());
    }
}
