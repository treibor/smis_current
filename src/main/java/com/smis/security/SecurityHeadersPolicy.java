package com.smis.security;

/** One policy definition shared by Spring's fallback and Vaadin's bootstrap writer. */
public final class SecurityHeadersPolicy {
    private SecurityHeadersPolicy() {}

    // PDF.js thumbnails use canvas.toDataURL; its image rendering uses blob URLs.
    // The PDF viewer print-js integration loads a PDF blob in a printing iframe.
    // Neither exception permits these URLs in stored CKEditor HTML.
    public static final String CSP = "default-src 'self'; base-uri 'self'; object-src 'none'; "
            + "frame-ancestors 'none'; form-action 'self'; "
            + "script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' blob: data:; font-src 'self' data:; connect-src 'self'; "
            + "frame-src 'self' blob:; media-src 'self'; manifest-src 'self'; worker-src 'self' blob:";

    public static String forBootstrap(String nonce) {
        if (!nonce.matches("[A-Za-z0-9+/]{43}=")) throw new IllegalArgumentException("Invalid nonce");
        return CSP.replace("script-src 'self'", "script-src 'self' 'nonce-" + nonce + "'")
                + "; require-trusted-types-for 'script'; trusted-types default dompurify lit-html "
                + "polymer-html-literal polymer-template-event-attribute-policy";
    }
}
