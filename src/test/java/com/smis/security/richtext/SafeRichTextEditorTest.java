package com.smis.security.richtext;

import static org.assertj.core.api.Assertions.assertThat;
import com.wontlost.ckeditor.VaadinCKEditor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SafeRichTextEditorTest {
    @Test
    void sanitizesLegacyContentBeforeSendingItToBrowser() {
        SafeRichTextEditor view = new SafeRichTextEditor();
        view.setValue("<p onclick='alert(1)'>Legacy<script>alert(2)</script></p>");
        VaadinCKEditor editor = (VaadinCKEditor) ReflectionTestUtils.getField(view, "editor");
        assertThat(editor.getElement().getProperty("editorData")).isEqualTo("<p>Legacy</p>");
    }

    @Test
    void sanitizesUntrustedEditorOutputForPersistenceAndReport() {
        SafeRichTextEditor view = new SafeRichTextEditor();
        VaadinCKEditor editor = (VaadinCKEditor) ReflectionTestUtils.getField(view, "editor");
        editor.setValue("<p>Notes<img src=x onerror=alert(1)></p>");
        assertThat(view.getValue()).isEqualTo("<p>Notes</p>");
    }
}
