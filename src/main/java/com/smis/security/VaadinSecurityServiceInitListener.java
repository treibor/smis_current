package com.smis.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/** Uses the Flow 24.2 index hook after AppShell and PWA scripts have been added. */
@Component
public final class VaadinSecurityServiceInitListener implements VaadinServiceInitListener {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final java.util.Map<String, String> SCRIPT_VERSIONS = scriptVersions();

    private static java.util.Map<String, String> scriptVersions() {
        var versions = new java.util.HashMap<String, String>();
        for (String name : new String[]{"purify.min.js", "csp-registry.js", "csp-compat.js"}) {
            try (var input = VaadinSecurityServiceInitListener.class.getResourceAsStream(
                    "/META-INF/resources/security/" + name)) {
                if (input == null) throw new IllegalStateException("Missing CSP resource: " + name);
                versions.put("./security/" + name, java.util.HexFormat.of().formatHex(
                        java.security.MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())));
            } catch (java.io.IOException | java.security.NoSuchAlgorithmException error) {
                throw new IllegalStateException("Cannot version CSP resources", error);
            }
        }
        return java.util.Map.copyOf(versions);
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String nonce = Base64.getEncoder().encodeToString(bytes);
            response.getDocument().getElementsByTag("script").attr("nonce", nonce);
            response.getDocument().getElementsByTag("script").forEach(script -> {
                String source = script.attr("src");
                String version = SCRIPT_VERSIONS.get(source);
                if (version != null) script.attr("src", source + "?v=" + version);
            });
            response.getVaadinResponse().setHeader("Content-Security-Policy", SecurityHeadersPolicy.forBootstrap(nonce));
            response.getVaadinResponse().setHeader("Cache-Control", "no-store");
        });
        event.addRequestHandler(new ValidatingUidlRequestHandler());
        event.getSource().addSessionInitListener(initialized -> initialized.getSession().setErrorHandler(error -> {
            String reference = ErrorReferences.record(error.getThrowable());
            com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
            if (ui != null) {
                com.vaadin.flow.component.notification.Notification.show(
                        "Unable to complete the request. Reference: " + reference);
            }
        }));
    }
}
