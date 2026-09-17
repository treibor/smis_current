package com.smis.security.richtext;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Backstop for repository saves, saveAll, dirty checking and cascaded writes. */
@Converter
public class RichTextHtmlConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String html) {
        return RichTextHtmlSanitizer.INSTANCE.sanitizeForStorage(html);
    }

    @Override
    public String convertToEntityAttribute(String html) {
        // Preserve raw legacy values for the explicit dry-run/maintenance process.
        // Every HTML presentation goes through the rendering boundary instead.
        return html;
    }
}
