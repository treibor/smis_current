# HTTP security headers — verification report

Verified 8 September 2026. Spring Boot 3.1.5, Spring Security 6.1.x, Vaadin 24.2.0, Java 17; context path `/smis-test`.

## Findings and scope

The application had one `VaadinWebSecurity` configuration with redundant static writers, enabled legacy XSS filtering, an invalid name-only Set-Cookie writer, obsolete Expect-CT, unconditional HSTS on HTTP and no enforced CSP. Static cache writers could override resource caching. A separate, previously implemented global Referrer-Policy filter already used `strict-origin-when-cross-origin` correctly.

Inspected application HttpSecurity/chain definitions, Vaadin 24.2.0's superclass source, header writers, servlet filters, session configuration, PWA/bootstrap, frontend dependencies, CSS, rich text, reports, images and charts. There is **one security chain, no separate API chain, and no `web.ignoring()` routes**. Vaadin 24.2 permits its static resources through the security chain. No Nginx configuration, external Tomcat context/server XML, application header interceptor, additional CSP/report-only writer, active upload endpoint, camera integration, or explicitly enabled `@Push` configuration was found.

`RateLimitingFilter` remains a servlet component. The method-rejection filter retains the same methods/statuses; its chain position now follows HeaderWriterFilter and precedes CSRF/authentication, so its 405 responses receive security headers. `super.configure(http)`, `setLoginView`, authorization rules, optional test-authentication filter, session concurrency, security context, CSRF and existing CORS configuration are preserved. No sanitizer/editor protection was loosened.

One error-path correction was necessary: permit **container ERROR dispatches**, so a missing public build resource returns its 404 instead of being redirected to login during `/error` dispatch. Direct client requests to `/error` still require normal authorization. This does not permit ordinary requests to protected routes.

### Observed existing deployment

A HEAD request to `http://10.179.2.248:8081/smis-test/login` returned 200 with:

* No CSP.
* `Referrer-Policy: same-origin` (the deployed version differs from the current workspace).
* `X-XSS-Protection: 1; mode=block` and Expect-CT.
* HSTS despite plain HTTP.
* An actual JSESSIONID with `Path=/smis-test; HttpOnly; SameSite=Strict`, **without Secure**.

No session identifier is retained in this report. The deployed server was inspected only, not updated. No HTTPS URL or proxy configuration was supplied. The WAR supports both embedded and external Tomcat (`SpringBootServletInitializer`, provided Tomcat dependency); the remote server's launch method cannot be established from that HTTP response.

Changing the deployed referrer policy to the previously requested `strict-origin-when-cross-origin` retains full same-origin referrers, permits origin-only referrers for non-downgrade cross-origin requests, and removes referrers on HTTPS-to-HTTP downgrade. No application workflow was found that depends on suppressing all cross-origin referrers. It is still a privacy behavior change from the currently deployed `same-origin` policy.

## Final header policy

Spring Security's built-in CSP, frame-options, HSTS and permissions-policy DSL supplies the headers. Default Spring writers supply nosniff, X-XSS-Protection=0 and conditional cache control. The existing sole global referrer writer remains unchanged.

```text
default-src 'self'; base-uri 'self'; object-src 'none';
frame-ancestors 'none'; form-action 'self';
script-src 'self' 'nonce-<fresh-response-nonce>';
style-src 'self' 'unsafe-inline';
img-src 'self' blob: data:;
font-src 'self' data:;
connect-src 'self';
frame-src 'self' blob:;
media-src 'self'; manifest-src 'self'; worker-src 'self' blob:;
require-trusted-types-for 'script';
trusted-types default dompurify lit-html polymer-html-literal polymer-template-event-attribute-policy
```

This is sent as exactly one **enforced Content-Security-Policy**, not report-only. The nonce is generated for each bootstrap response by the Flow index listener; non-bootstrap responses omit the nonce and Trusted Types directives. Spring's CSP writer preserves the policy already set by the listener. See [the CSP and UIDL remediation report](csp-uidl-remediation.md) for compatibility mappings, tests and deployment acceptance requirements. Inline styles remain allowed; inline scripts and runtime string compilation do not.

The only URL-scheme exceptions beyond self are tied to inspected components:

| Exception | Evidence / reason |
| --- | --- |
| `img-src data:` | The active `vcf-pdf-viewer` creates `PDFThumbnailViewer`; `pdfjs/dist/pdf_thumbnail_viewer.js` assigns `reducedCanvas.toDataURL()` to thumbnail `image.src`. |
| `img-src blob:` | Bundled PDF.js creates image object URLs for rendered PDF image data. No application image-upload preview was found. |
| `font-src data:` | Bundled PDF.js supports generated embedded-font data URLs. Normal application fonts are local. |
| `worker-src blob:` | Bundled PDF.js worker support includes blob wrappers. Same-origin workers remain allowed. |
| `frame-src blob:` | The PDF viewer's print integration uses print-js; `print-js/src/js/pdf.js` creates a PDF object URL and assigns it to the printing iframe. |

`object-src 'none'` remains intact: the viewer renders through PDF.js/canvas and its print integration uses an iframe. These CSP exceptions **do not allow data/blob/external images in stored CKEditor content**; its sanitizer and disabled insertion/upload features remain unchanged.

No external hosts, `*`, broad `https:`, `ws:` or `wss:` source allowances are added. Requests are same-origin. There is no active application `@Push`/custom WebSocket origin to justify unrestricted WebSockets. Atmosphere infrastructure is preserved. If deployment later enables cross-origin Push, supply the exact approved endpoint and test that endpoint/browser combination rather than adding broad schemes.

Other headers:

| Header | Result |
| --- | --- |
| Referrer-Policy | `strict-origin-when-cross-origin`, one value |
| X-Content-Type-Options | `nosniff` |
| X-Frame-Options | `DENY`, consistent with `frame-ancestors 'none'` |
| X-XSS-Protection | `0` via modern default writer |
| Permissions-Policy | `geolocation=(self), microphone=()`; no camera feature found, no extra camera restriction added |
| Strict-Transport-Security | `max-age=31536000` on secure requests only; no includeSubDomains because domain/subdomain HTTPS coverage is unknown |
| Cache-Control / Pragma | Spring defaults for uncached dynamic responses; existing resource cache headers preserved |
| Expect-CT / fake Set-Cookie / report-only CSP | Removed / absent |

Live checks on the production WAR confirmed `max-age=3600` for hashed Vaadin JavaScript and existing images; bootstrap and errors remain no-store. No attempt was made to change the application's resource cache duration.

## Actual session cookie and HTTPS deployment

`SessionCookieConfiguration` configures the **Servlet 6 SessionCookieConfig** at context initialization, rather than emitting a response-header fragment. This common servlet mechanism applies to embedded and external Tomcat 10.1:

* HttpOnly=true.
* Secure=true by default.
* SameSite=Strict on the actual cookie.
* Path from the deployed servlet context (`/smis-test` here), not a hard-coded root path.

Tomcat 10.1 supports the cookie attributes through its [cookie processor](https://tomcat.apache.org/tomcat-10.1-doc/config/cookie-processor.html); context/session path behavior is described in [Tomcat's Context reference](https://tomcat.apache.org/tomcat-10.1-doc/config/context.html). Inspect container-wide settings when deploying: an administrator's cookie processor/context policy must not conflict with Strict or the application context path.

**The supplied deployment URL uses HTTP. Secure cookies cannot provide working authenticated sessions over that remote HTTP URL. Configure HTTPS before deploying these production defaults.** The code intentionally does not pretend that HSTS makes HTTP secure. An explicit `server.servlet.session.cookie.secure=false` override is available only for deliberate local HTTP testing; do not use it as the production remediation.

No forwarded-header trust was enabled blindly. If TLS terminates at a reverse proxy:

1. Restrict backend access to the trusted proxy and have it replace incoming forwarding headers with authoritative values.
2. For external Tomcat, configure a RemoteIpValve with the **actual trusted proxy addresses**, `protocolHeader="x-forwarded-proto"` and the appropriate forwarded address header. Alternatively configure that dedicated connector's known scheme/secure/proxy port settings.
3. For embedded Tomcat, use `server.forward-headers-strategy=native` with an explicitly reviewed `server.tomcat.remoteip.internal-proxies` pattern and protocol-header settings. Do not trust arbitrary client-supplied headers.
4. Verify the public HTTPS URL produces Secure cookies and HSTS. The supplied infrastructure information is insufficient to claim that this proxy setup has been completed.

## Tests and production smoke checks

`.\mvnw.cmd -Pproduction clean package` passed with **52 tests, zero failures/errors/skips**. The build was run in the isolated verification copy to avoid changing tracked generated frontend files. Windows sandbox restrictions on spawning Vite required an authorized retry. A later rebuild required stopping the verification server because Windows locked its open WAR.

* Expanded `ReferrerPolicyTest`: public resource GET/HEAD, authenticated test route, authentication redirect, access-denied 403, method-rejection 405, real Tomcat 404/500 ERROR dispatches, exact single CSP/referrer/frame/nosniff/XSS headers, obsolete/fake-header absence, HTTPS-only HSTS and preservation of explicit resource caching.
* Added `HttpsSessionCookieTest`: temporary localhost TLS certificate trusted explicitly by the test client; real HTTPS Tomcat session under `/smis-test`, actual JSESSIONID attributes, production security-chain form login, authenticated request and logout. This uses an in-memory test account and does not claim a browser login to the deployed database account.
* All existing authentication-profile and stored-XSS regression tests passed.
* Added `scripts/verify-security-headers.ps1` for live production bootstrap/resource checks. It discovered the actual hashed bundle from `/login`; GET and HEAD returned 200 with exactly one expected header value and one-hour caching. Existing image GET/HEAD passed. A nonexistent `/VAADIN/build/**.js` returned 404 with the same security headers.

The actual production WAR was started on loopback ports with database connections forced read-only and SQL initialization disabled. Browser checks under enforced CSP passed for:

| Feature | Result |
| --- | --- |
| Normal login page/bootstrap | Loaded; no warning/error or CSP violation in captured console logs |
| Authenticated navigation/home | Passed using the existing loopback test-authentication profile, visibly marked TEST MODE |
| CKEditor | Loaded and accepted editing; supported toolbar appeared; no console errors; no database save performed in this read-only browser run |
| Charts | `/stats` bar and pie charts rendered; no console warnings/errors |
| Dialog / notification | About dialog and report validation notification worked |
| Images | Production image GET/HEAD passed; local bootstrap icons rendered |
| 404 resource | Live executable-WAR test passed after the ERROR-dispatch correction |
| PDF/report preview/download | **Blocked by pre-existing report-loader `FileSystemNotFoundException`** when converting a classpath resource to a filesystem path in the executable WAR. No CSP violation occurred. The component's blob/data requirements were verified in source, but a complete PDF preview/download/print test remains necessary in the intended expanded-WAR deployment. |
| CKEditor image upload / application file upload | Not applicable: editor image insertion/upload is intentionally disabled; no active application upload feature found |
| Push/WebSocket messaging | No enabled application Push feature found; infrastructure preserved, no end-to-end WebSocket claim |
| Real-account browser login/logout | Not performed; production-chain HTTPS login/logout tested with a fixture account, and normal login UI separately smoke-tested |

The browser checks did not write business data. Test-authentication mode was used only on a loopback server, not on the deployment server. No code or container configuration was deployed remotely.

Note: missing `/images/*.png` paths can be handled as a Vaadin bootstrap fallback with status 200 in the executable WAR. They still receive the headers. The genuine 404 verification uses a nonexistent Vaadin build resource; this report does not mislabel the fallback as a 404 or alter general Vaadin routing to satisfy a scanner.

## Files changed in this request

* `src/main/java/com/smis/security/SecurityConfiguration.java` — built-in header DSL, method-filter position and container ERROR authorization.
* `src/main/java/com/smis/security/SecurityHeadersPolicy.java` — documented CSP constant.
* `src/main/java/com/smis/security/SessionCookieConfiguration.java` — actual servlet session-cookie configuration.
* `src/test/java/com/smis/security/ReferrerPolicyTest.java` — expanded regression coverage.
* `src/test/java/com/smis/security/HttpsSessionCookieTest.java` — HTTPS session/login/logout regression.
* `scripts/verify-security-headers.ps1` and this report.

The existing global referrer filter, rich-text sanitizer, editor restrictions, prior AppScan investigation and other pre-existing working-tree changes are preserved. No generated/minified files were edited manually.

Separate generated-file churn appeared in the original checkout while Eclipse was open (including removed `frontend/generated/flow` chunks and generated entry points). Its origin was not established conclusively; those files were left untouched rather than restoring potentially concurrent IDE/user work. The verified artifact was built from the isolated copy.

## Deployment verification

After configuring HTTPS and deploying the new WAR, run the script using the actual public HTTPS URL:

```powershell
.\scripts\verify-security-headers.ps1 -BaseUrl 'https://YOUR-HTTPS-HOST/smis-test'
```

Manual checks (substitute the actual host and deployed bundle hash):

```bash
curl -I https://YOUR-HTTPS-HOST/smis-test/login
curl -I https://YOUR-HTTPS-HOST/smis-test/images/plant.png
curl -I https://YOUR-HTTPS-HOST/smis-test/VAADIN/build/indexhtml-HASH.js
curl -I https://YOUR-HTTPS-HOST/smis-test/VAADIN/build/nonexistent-00000000.js
```

Use browser Network tools on an actual login/session response to verify JSESSIONID's Secure, HttpOnly, SameSite=Strict and Path=/smis-test. Check login/logout, PDF/report preview/print/download and any deployment-only resources in the intended Tomcat/proxy environment. Do not add extra proxy-level header writers that duplicate Spring's headers. No broad CSP relaxation is justified by the current findings.
