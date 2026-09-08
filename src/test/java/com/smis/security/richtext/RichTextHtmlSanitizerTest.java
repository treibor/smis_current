package com.smis.security.richtext;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RichTextHtmlSanitizerTest {
    private final RichTextHtmlSanitizer sanitizer = new RichTextHtmlSanitizer();

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert(1)</script>", "<img src=x onerror=alert(1)>",
        "<a href='javascript:alert(1)'>Click</a>", "<a href='jAvAsCrIpT:alert(1)'>Click</a>",
        "<a href='jav&#x61;script:alert(1)'>Click</a>", "<a href='java&#9;script:alert(1)'>Click</a>",
        "<a href='&#106;&#97;vascript:alert(1)'>Click</a>", "<a href='vbscript:alert(1)'>Click</a>",
        "<svg onload=alert(1)></svg>", "<iframe srcdoc='<script>alert(1)</script>'></iframe>",
        "<object data='javascript:alert(1)'></object>", "<meta http-equiv=refresh content='0;url=javascript:alert(1)'>",
        "<div onclick=alert(1)>Click</div>", "<form><button formaction='javascript:alert(1)'>Submit</button></form>",
        "<math><mtext><img src=x onerror=alert(1)></mtext></math>",
        "<p><strong>safe<img src=x onerror=alert(1)",
        "<svg><foreignObject><p onmouseover=alert(1)>x</p></foreignObject></svg>",
        "<img src='data:image/svg+xml;base64,PHN2Zz4='>", "<img src='data:image/png;base64,aGVsbG8='>",
        "<img src='blob:https://example.com/id'>", "<img src='file:///etc/passwd'>",
        "<img src='https://unapproved.example/a.png'>", "<img src='//evil.example/a.png'>",
        "<img src='/images/../secret.png'>", "<img src='/images/%2e%2e/secret.png'>",
        "<div style='background-image:url(javascript:alert(1));position:fixed;behavior:url(x);color:red'>x</div>",
        "<span style='width:expression(alert(1));color:red' id=x class=anything title=x>x</span>",
        "<style>body{display:none}</style><link href=x><base href='https://evil.example/'>",
        "<a href='file:///etc/passwd' target='_blank' ping='https://evil.example/'>x</a>",
        "<table><tr><td><math><mtext><img src=x onerror=alert(1)></table>"
    })
    void removesActiveMarkupAndIsIdempotent(String input) {
        String safe = sanitizer.sanitize(input);
        var body = Jsoup.parseBodyFragment(safe).body();
        assertThat(body.select("script,iframe,object,embed,svg,math,form,input,button,meta,link,base,style")).isEmpty();
        body.getAllElements().forEach(element -> element.attributes().forEach(attribute -> {
            String key = attribute.getKey().toLowerCase(Locale.ROOT);
            assertThat(key.startsWith("on")).isFalse();
            assertThat(key).isNotIn("srcdoc", "target", "ping", "id");
            if (List.of("href", "src").contains(key)) {
                assertThat(attribute.getValue().toLowerCase(Locale.ROOT))
                        .doesNotStartWith("javascript:").doesNotStartWith("vbscript:")
                        .doesNotStartWith("file:").doesNotStartWith("data:").doesNotStartWith("blob:");
            }
            if (key.equals("src")) assertThat(attribute.getValue()).isIn("/images/plant.png",
                    "/images/empty-plant.png", "images/plant.png", "images/empty-plant.png");
            if (key.equals("style")) assertThat(attribute.getValue()).doesNotContain("url(", "expression", "position", "behavior");
        }));
        assertThat(sanitizer.sanitize(safe)).isEqualTo(safe);
    }

    @Test
    void preservesRequiredFormattingTablesAndApprovedImage() {
        String safe = sanitizer.sanitize("""
                <h2>Title</h2><p><strong>Bold</strong><em>Italic</em><u>Underline</u><s>Strike</s><sub>sub</sub><sup>sup</sup></p>
                <ol start="3"><li>First</li></ol><ul><li>Bullet</li></ul><blockquote>Quote</blockquote><pre><code>x</code></pre>
                <figure class="table"><table><thead><tr><th colspan="2">Heading</th></tr></thead><tbody><tr><td rowspan="2">Cell</td></tr></tbody></table></figure>
                <p><span style="font-family:'Times New Roman', Times, serif;font-size:14px;color:rgb(10, 20, 30);text-align:center">Copy To:</span></p>
                <a href="https://example.com" title="Link">Link</a><a href="mailto:a@example.com">Mail</a><a href="tel:+12345">Call</a>
                <img src="/images/plant.png" alt="Plant" width="64" height="64">
                """);
        var body = Jsoup.parseBodyFragment(safe).body();
        for (String selector : List.of("h2", "strong", "em", "u", "s", "sub", "sup", "ol[start=3]", "ul li",
                "blockquote", "pre code", "figure.table table", "th[colspan=2]", "td[rowspan=2]", "img[src=/images/plant.png]")) {
            assertThat(body.select(selector)).as(selector).isNotEmpty();
        }
        assertThat(body.selectFirst("span").attr("style")).contains("font-family", "font-size", "color", "text-align");
        assertThat(body.select("a")).hasSize(3);
        assertThat(sanitizer.sanitize(safe)).isEqualTo(safe);
    }

    @Test
    void boundsNullBlankAndLargeValuesWithoutEchoingContent() {
        assertThat(sanitizer.sanitize(null)).isNull();
        assertThat(sanitizer.sanitize(" \n\t")).isEmpty();
        String huge = "<script>secret</script>".repeat(5000);
        assertThatThrownBy(() -> sanitizer.sanitize(huge)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("secret");
        assertThatThrownBy(() -> sanitizer.sanitizeForStorage("x".repeat(2001))).isInstanceOf(IllegalArgumentException.class);
        assertThat(sanitizer.sanitizeForStorage("x".repeat(2000))).hasSize(2000);
        assertThat(sanitizer.sanitizeForRendering(huge)).isEmpty();
        assertThat(sanitizer.sanitizeForRendering(null)).isEmpty();
    }
}
