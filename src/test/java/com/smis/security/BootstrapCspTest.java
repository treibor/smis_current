package com.smis.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.vaadin.flow.server.*;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class BootstrapCspTest {
    @Test void bootstrapHasFreshMatchingNonceAndOneEnforcedPolicy() {
        var listener = new VaadinSecurityServiceInitListener();
        var event = new ServiceInitEvent(mock(VaadinService.class));
        listener.serviceInit(event);
        var hook = event.getAddedIndexHtmlRequestListeners().findFirst().orElseThrow();
        String previous = null;
        for (int i = 0; i < 20; i++) {
            var response = mock(IndexHtmlResponse.class);
            var http = mock(VaadinResponse.class);
            var document = Jsoup.parse("<script>trusted()</script><script src='/VAADIN/build/app.js' type='module'></script>");
            when(response.getDocument()).thenReturn(document);
            when(response.getVaadinResponse()).thenReturn(http);
            hook.modifyIndexHtmlResponse(response);
            String nonce = document.selectFirst("script").attr("nonce");
            assertThat(nonce).matches("[A-Za-z0-9+/]{43}=").isNotEqualTo(previous);
            assertThat(document.select("script")).allMatch(script -> script.attr("nonce").equals(nonce));
            String policy = SecurityHeadersPolicy.forBootstrap(nonce);
            assertThat(policy).doesNotContain("script-src 'self' 'unsafe-inline'");
            if (previous != null) assertThat(policy).doesNotContain(previous);
            verify(http).setHeader("Content-Security-Policy", policy);
            verify(http).setHeader("Cache-Control", "no-store");
            verifyNoMoreInteractions(http);
            previous = nonce;
        }
    }
}
