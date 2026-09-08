package com.smis.security;

/** Enforced baseline for ordinary Vaadin 24.2 bootstrap; not nonce-based strict CSP. */
public final class SecurityHeadersPolicy {
    private SecurityHeadersPolicy() {}

    // PDF.js thumbnails use canvas.toDataURL; its image rendering uses blob URLs.
    // The PDF viewer print-js integration loads a PDF blob in a printing iframe.
    // Neither exception permits these URLs in stored CKEditor HTML.
    public static final String CSP = "default-src 'self'; base-uri 'self'; object-src 'none'; "
            + "frame-ancestors 'none'; form-action 'self'; "
            + "script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' blob: data:; font-src 'self' data:; connect-src 'self'; "
            + "frame-src 'self' blob:; media-src 'self'; manifest-src 'self'; worker-src 'self' blob:";
}
