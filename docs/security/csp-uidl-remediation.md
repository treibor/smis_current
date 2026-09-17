# CSP and UIDL remediation — 2026-09-11

## Release status

**Workspace remediation; deployment rescan required.** Script `unsafe-inline` and `unsafe-eval` are removed. Bootstrap HTML enforces Trusted Types. Invalid UIDL numeric types/ranges are rejected before Flow's narrowing conversion. The component fixture covers representative interactions; the full authenticated deployment matrix still needs acceptance testing.

The original working tree was clean. Checkpoint: `32d44f6` (`chore: checkpoint before CSP and UIDL security remediation`). No AGENTS.md was found in the repository. README deployment guidance was followed. Vaadin is **24.2.0 before and after**, Spring Boot 3.1.5, Java target 17; the request's Vaadin 25 premise does not match this checkout. Tests/build used installed JDK 17, not the default JDK 25.

## Evidence and root causes

### Inline scripts

`SecurityConfiguration` is the only original executable CSP writer and uses `SecurityHeadersPolicy.CSP`. Searches covered application sources, resources, frontend, scripts and repository deployment documentation. No Nginx configuration or other deployment CSP writer was found. External proxy/container configuration is not available here and must be checked on deployment.

The original policy explicitly authorized any inline script. This is a defence-in-depth weakness: it could amplify an injection vulnerability, but the policy itself does not establish an injection route. Existing rich-text allowlist sanitization remains in place and its regression tests pass.

The Flow index hook now generates 32 bytes using `SecureRandom`, Base64 encodes them, attaches the nonce to all bootstrap script elements, sets the enforced header and marks bootstrap HTML `no-store`. The hook runs after Flow adds AppShell/PWA scripts; Flow clones its cached document for each response. Spring's fallback writer does not replace an existing CSP header. There is one effective header on the tested real HTML responses; ordinary non-bootstrap responses have the baseline without a nonce.

Official references: [Vaadin 24 CSP guidance](https://vaadin.com/docs/v24/flow/security/advanced-topics/strict-csp), [Flow 24.2.0 source](https://github.com/vaadin/flow/tree/24.2.0). Exact implementation was checked in the local Maven `flow-server-24.2.0-sources.jar` and `flow-client-24.2.0-sources.jar`, including `IndexHtmlRequestHandler`, `ServerRpcHandler`, `UidlRequestHandler`, `VaadinService`, `MessageSender` and `ExecuteJavaScriptProcessor`.

### UIDL clientId

`ServerRpcHandler.RpcRequest` lines 120–125 in 24.2.0 reads a JSON double and casts to `int`. `4294967297` saturates to `2147483647`; it does **not** wrap to 1 in this Java conversion. `handleRpc` compares that result to the expected counter and throws `UnsupportedOperationException` at line 313 before invoking RPCs. The regression test reproduces that exception and verifies its origin without recording the token/body.

`MessageSender` has an `int clientToServerMessageId = 0` and sends `clientToServerMessageId++`. The server also increments a signed int. The representable range is therefore **-2147483648 through 2147483647**, including rollover; merely declaring all negatives invalid would change the protocol. `-1` and a missing field also have explicit legacy sentinel behavior in `RpcRequest`/`handleRpc`. They are preserved. In-range but unexpected sequence numbers still follow Flow's normal sequence handling; this change does not disable ordering or CSRF checks.

Official Maven Central metadata lists 24.2.13 as the newest 24.2 patch. Its source archive was downloaded and inspected: the same cast and unexpected-ID exception remain. No upgrade was made because this patch does not fix the issue. This is not a claim that the old 24.2 line is currently supported; migration to a newer supported minor/major requires separate dependency and add-on compatibility verification.

This is an input-validation/availability hardening issue, not demonstrated disclosure or integer-index exploitation. The supplied generic `appError` does not disclose internal details. Source inspection and tests found no RPC invocation or counter update for the reproduced oversized request. A blanket false-positive conclusion is unwarranted because avoidable narrowing and an internal exception do occur.

`ValidatingUidlRequestHandler` is registered ahead of the built-in handler through Flow's service-init API. It subclasses the version-pinned UIDL handler and validates its Reader before passing the unchanged message through `StringReader` to the original RPC implementation. It intercepts only UIDL POSTs, after UI resolution. It leaves expired/missing UIs to Flow's existing recovery response. There is no servlet wrapper, session-token replacement or alternate RPC implementation.

Malformed JSON, duplicate top-level IDs, decimals (including `1.0`), strings, null, and integers outside the signed range return HTTP **400**, `text/plain;charset=UTF-8`, exactly `Invalid request`. Reads are bounded to **1,048,576 characters** before parsing. This new UIDL limit exceeds the existing 100,000-character RTE limit, but unusually large batched RPCs still require deployment testing. Uploads use a separate transport and are not buffered by this handler. Empty bodies and missing IDs preserve Flow's explicit compatibility behavior.

Parser exception messages are discarded because they can contain input excerpts. No complete UIDL body or token is logged. The tests send invalid requests followed by a valid request using the same real production-mode UI/session; the valid request still returns a normal UIDL response.

## Exact policies

Before:

```text
default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' blob: data:; font-src 'self' data:; connect-src 'self'; frame-src 'self' blob:; media-src 'self'; manifest-src 'self'; worker-src 'self' blob:
```

After, bootstrap HTML (`<NONCE>` is the fresh 44-character Base64 value):

```text
default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; script-src 'self' 'nonce-<NONCE>'; style-src 'self' 'unsafe-inline'; img-src 'self' blob: data:; font-src 'self' data:; connect-src 'self'; frame-src 'self' blob:; media-src 'self'; manifest-src 'self'; worker-src 'self' blob:; require-trusted-types-for 'script'; trusted-types default dompurify lit-html polymer-html-literal polymer-template-event-attribute-policy
```

Non-bootstrap fallback omits the nonce and both Trusted Types directives. Worker responses must not inherit the document's Trusted Types requirement: the service worker has its own execution context without the document policy. The fallback still prohibits unsafe-eval. No report-only header, wildcard, script data URL or added third-party origin is used.

## Dynamic JavaScript inventory and remaining exceptions

| Surface | Evidence / disposition |
|---|---|
| Application Page.executeJs | Only commented-out code in LoginOld; no active direct application call found. Framework calls remain. |
| Flow connectors | Exact precompiled bodies and constrained method wrappers replace string compilation. Grid/ComboBox data updates and event callbacks are exercised in the fixture. |
| Lazy chunks | Compatibility module recognizes a fully anchored loadOnDemand call with a constrained chunk identifier and invokes the existing loader. No generated script tags or nonce-based eval bypass. |
| Charts | Actual dependency is SO Charts 3.2.4, used in HomeView and StatsView, not ApexCharts. Generated `so/chart/chart.js:167` constructs functions from `o.function.params`/`o.function.body`. These dynamic configurations are not fully inventoried at runtime. |
| Grid export / preview | xdev grid exporter 3.0.3 and vcf PDF viewer 3.0.0. Existing image blob/data and frame blob allowances retained; complete export/print workflow untested. |
| Rich-text editor | CKEditor add-on 4.0.0, wrapped by SafeRichTextEditor. Existing OWASP explicit elements/attributes/protocol/CSS allowlists remain; script, handlers, executable URLs, SVG, iframe/object are excluded. Persistence/converter/editor tests pass. |
| Dialogs and public/image views | Dialog open/close callbacks exercised. No active application executeJs found. Full protected navigation remains a deployment acceptance check. |
| Idle notification / Push | No explicit Idle Notification dependency or active @Push annotation found. The UIDL hook does not intercept heartbeat or Push; transport matcher tests cover exclusions. Real WebSocket/session timeout behavior remains untested. |

`purify.min.js`, `csp-registry.js`, then `csp-compat.js` load synchronously before Flow modules. Their URLs include a SHA-256 content version to invalidate cached scripts after upgrades. Only these exact resources are anonymously permitted by Spring Security. The registry precompiles dependency constants, annotation event expressions and the application's Enter/Escape shortcuts at build time. Runtime wrappers accept only exact registered bodies and constrained framework call shapes; unknown code fails closed with a fingerprint. There is no native compilation fallback or createScript policy. New components, custom chart functions, or dependency upgrades require regenerated mappings and browser regression tests.

Trusted Types uses DOMPurify 3.4.15 (vendored with its license) for the default HTML policy. It removes executable markup rather than returning arbitrary HTML unchanged. Script URLs are restricted to same-origin JavaScript under the context's VAADIN directory and its service worker. The permitted Lit/Polymer policy names match bundled framework code. DOMPurify npm tarball integrity: `sha512-EUBjM+B+lkDE41iE82DDSCfkoPGfXx8IxFxPMjNzm/Uk4xDet77rTN9wqlxlVg71kK7XGuUMv6wUxJUwwv+Xyw==`.

To regenerate against resolved dependencies: run Maven `dependency:build-classpath -Dmdep.outputFile=target/security-classpath.txt`, download matching dependency sources, run `scripts/collect-csp-sources.ps1`, then JDK 17 `java scripts/CollectCspConstants.java`, then `node scripts/build-csp-registry.cjs`. Review `csp-expression-inventory.json`; never add expressions captured from untrusted requests automatically. Run `node scripts/test-csp-compat.cjs` with string code generation disabled, and the production browser fixture after every regeneration.

`style-src 'unsafe-inline'` remains because the index itself contains inline layout CSS and Vaadin/Lumo, application component styles and sanitized CKEditor formatting use runtime inline styles. A nonce on scripts does not authorize style attributes. Styling without this exception has not been demonstrated.

## Error handling

Vaadin session errors and Spring error attributes now issue generic messages with a UUID reference. Server logs retain exception types and originating stack frames under that reference. Exception/cause messages are deliberately excluded because they can contain SQL parameters or tokens. Spring response options explicitly exclude messages, binding errors, exception names and stack traces; the custom attributes also ignore requested debug inclusions. Logging regression tests verify that synthetic secrets in nested exception messages do not appear.

This reviews central handlers; it does not claim to audit every infrastructure/access logger or every application-specific catch block. Do not enable request-body, authorization-header or cookie tracing in the proxy/container.

## Verification and limitations

Baseline: 54 tests passed. Updated suite: **76 tests, zero failures/errors/skips**, including three production HTTP tests. Added tests cover nonce uniqueness/matching and header count; actual production HTML and anonymous script delivery; malformed and boundary IDs through the real Flow handler and HTTP; original exception reproduction; valid RPC progression; transport exclusions; bounded/duplicate/malformed input; and sanitized error references. The JavaScript compatibility test runs with VM string code generation disabled. Build results are in `target/strict-csp-final-build.log`; subsequent verification is in `target/strict-csp-verification.log`.

Production Maven packaging succeeded (WAR `target/smis-2.0.war`). Vite needed execution outside the restricted sandbox to spawn its helper. The isolated production-mode browser rendered the login form and processed interaction with no captured error/warning console entries. No production database or existing user data was used.

The isolated H2 fixture verifies Grid rows, ComboBox selection, dialog open/close callbacks, SOChart bar rendering, rich-text toolbar/readback and the Enter shortcut's server callback. Nine real-browser probes passed: Trusted Types availability; rejection of native string compilation, unmapped Function, string eval, unapproved policy, external script URL and inline script sink; preservation of safe formatting; removal of executable HTML. Expected probe failures emit CSP diagnostics. It is a test-classpath route and is excluded from the WAR. The final WAR was checked against the current registry source and for absence of the fixture, probe script and test chunk list. Six HTTP header/asset checks passed. Not verified: authenticated successful login/logout, session timeout, WebSockets, all protected navigation, reports/export/print/upload/gallery/public-page journeys, mobile APIs, and an injected script using an old nonce. Nonce freshness is tested, but that is not a substitute for all browser journeys. No security scanner was run. These remain deployment acceptance requirements.

## Build, deploy and rollback

Changed files:

* `SecurityHeadersPolicy.java`: shared baseline and nonce policy construction.
* `VaadinSecurityServiceInitListener.java`: nonce/bootstrap hook, UIDL handler registration, generic session error handling.
* `ValidatingUidlRequestHandler.java`: bounded protocol validation before original RPC processing.
* `ErrorReferences.java`, `SafeErrorAttributes.java`, `application.properties`: generic responses and safe correlated server diagnostics.
* `frontend/index.html`, `META-INF/resources/security/csp-compat.js`: early compatibility module and initial function mappings.
* `BootstrapCspTest`, `ProductionBootstrapTest`, `UidlValidationTest`, `ErrorReferencesTest`: focused and HTTP regression coverage.
* `scripts/test-csp-compat.cjs`, `scripts/verify-security-headers.ps1`: mapping tests and updated deployed header checks.
* This report and `http-security-headers.md`: policy, evidence, limitations and operational instructions.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\mvnw.cmd package -Pproduction
.\mvnw.cmd compiler:testCompile surefire:test '-Dsmis.production-test=true'
node scripts/test-csp-compat.cjs
.\scripts\verify-security-headers.ps1 -BaseUrl 'https://HOST/smis-test'
```

Keep the existing WAR as a rollback artifact. Review the remaining eval exception and untested journeys before release. Deploy only the production WAR to Tomcat 10.1 following README: stop/undeploy the context, replace the WAR and old expanded directory, then restart. Ensure the proxy passes the application's sole CSP header and never caches/reuses bootstrap HTML. Preserve HTTPS Secure/HttpOnly/SameSite session cookies. No database migration is introduced.

Rollback: stop/undeploy, restore the previous known-good WAR and replace the expanded deployment, then restart. Source baseline is checkpoint `32d44f6`; use a separate checkout or reviewed revert, not a destructive reset of subsequent user work. Clear stale PWA/site data if an old frontend persists. Rollback also restores the original scan weaknesses.

## Deployed checks

```powershell
curl.exe -sS -D first.headers -o first.html https://HOST/smis-test/login
curl.exe -sS -D second.headers -o second.html https://HOST/smis-test/login
```

Confirm one enforced CSP per response, fresh nonces, all bootstrap script attributes matching that response's header, no script unsafe-inline, and no-store. Expect unsafe-eval until the remaining mapping work is completed. Treat saved HTML/headers as sensitive: they may contain session/CSRF data; do not attach them to public tickets.

For the UIDL scan, capture a valid test-session UIDL request in DevTools and keep cookie/token/body files locally with restricted permissions. Change only clientId in a copy of the body, then send it without verbose logging:

```powershell
curl.exe -sS -o invalid-response.txt -w '%{http_code}' -b test-cookies.txt -H 'Content-Type: application/json' --data-binary '@invalid-uidl.json' 'https://HOST/smis-test/?v-r=uidl&v-uiId=TEST_UI_ID'
```

With a live UI, invalid types/out-of-range integers must yield 400 and `Invalid request`; then send a valid next RPC and verify normal operation. Test 2147483647 as a representable boundary, not as the expected next ID of an arbitrary session. Missing/-1 and rollover values follow the protocol behavior described above.

In DevTools, preserve console/network logs while exercising each application journey; investigate every CSP violation and unmapped-expression fingerprint. Rescan both findings with a real session and archive redacted results. Verify the deployed HTML header has no script unsafe-eval and contains enforced `require-trusted-types-for 'script'`.
