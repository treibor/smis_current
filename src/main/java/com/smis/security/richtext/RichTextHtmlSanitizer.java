package com.smis.security.richtext;

import java.util.List;
import java.util.Set;
import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** The sole HTML policy for editor content, persistence, reports and maintenance. */
@Service
public final class RichTextHtmlSanitizer {
    public static final int MAX_INPUT_LENGTH = 100_000;
    public static final int MAX_STORED_LENGTH = 2_000;
    private static final Logger LOG = LoggerFactory.getLogger(RichTextHtmlSanitizer.class);
    private static final Set<String> IMAGES = Set.of("/images/plant.png", "/images/empty-plant.png",
            "images/plant.png", "images/empty-plant.png");
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "div", "h1", "h2", "h3", "h4", "h5", "h6",
                    "strong", "b", "em", "i", "u", "s", "sub", "sup", "ul", "ol", "li",
                    "blockquote", "pre", "code", "table", "thead", "tbody", "tfoot", "tr",
                    "th", "td", "a", "img", "span", "figure", "figcaption", "hr")
            .allowWithoutAttributes("span", "a", "div")
            .allowUrlProtocols("http", "https", "mailto", "tel")
            .allowAttributes("href", "title").onElements("a")
            .allowAttributes("src").matching((element, attribute, value) -> IMAGES.contains(value) ? value : null)
                .onElements("img")
            .allowAttributes("alt", "title").onElements("img")
            .allowAttributes("width", "height").matching((e, a, v) -> boundedInteger(v, 1, 2000)).onElements("img")
            .allowAttributes("colspan", "rowspan").matching((e, a, v) -> boundedInteger(v, 1, 100)).onElements("th", "td")
            .allowAttributes("start").matching((e, a, v) -> boundedInteger(v, -10000, 10000)).onElements("ol")
            .allowAttributes("class").matching((e, a, v) -> Set.of("table", "image").contains(v) ? v : null)
                .onElements("figure")
            .allowStyling(CssSchema.withProperties(List.of("font-family", "font-size", "font-weight",
                    "font-style", "text-decoration", "text-align", "color", "background-color", "line-height",
                    "rgb()", "rgba()", "hsl()", "hsla()")))
            .toFactory();

    // Converters also use this policy outside a Spring application context.
    public static final RichTextHtmlSanitizer INSTANCE = new RichTextHtmlSanitizer();

    public String sanitize(String untrustedHtml) {
        if (untrustedHtml == null) return null;
        if (untrustedHtml.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("Rich text exceeds the 100000-character input limit.");
        }
        String safe = untrustedHtml.isBlank() ? "" : POLICY.sanitize(untrustedHtml);
        if (!safe.equals(untrustedHtml)) {
            // Normalization is not evidence of an attack. Never log the content.
            LOG.info("Rich text normalized: inputCharacters={}, outputCharacters={}", untrustedHtml.length(), safe.length());
        }
        return safe;
    }

    public String sanitizeForStorage(String html) {
        String safe = sanitize(html);
        if (safe != null && safe.length() > MAX_STORED_LENGTH) {
            throw new IllegalArgumentException("Formatted rich text exceeds the 2000-character storage limit.");
        }
        return safe;
    }

    /** Fail closed for oversized legacy rows without breaking the whole view. */
    public String sanitizeForRendering(String html) {
        if (html != null && html.length() > MAX_INPUT_LENGTH) {
            LOG.warn("Oversized legacy rich text withheld from rendering: characters={}", html.length());
            return "";
        }
        String safe = sanitize(html);
        return safe == null ? "" : safe;
    }

    private static String boundedInteger(String value, int min, int max) {
        try {
            if (value.length() > 6) return null;
            int number = Integer.parseInt(value);
            return number >= min && number <= max ? Integer.toString(number) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
