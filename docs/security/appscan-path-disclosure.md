# AppScan: Possible Server Path Disclosure Pattern Found

Investigated on 2026-09-08. **Disposition: likely false positive for the reported JavaScript URL. No application or build-configuration change is justified by this finding.**

## Evidence and conclusion

The affected response was fetched directly from:

`http://10.179.2.248:8081/smis-test/VAADIN/build/vaadin-custom-field-24463f6f.js`

It returned HTTP 200, `Content-Type: text/javascript;charset=utf-8`, and 2,862,986 bytes. Its SHA-256 is:

```text
C2DE1B59A5F133E9BE1BADF1443EAB4E5B2A5884B1C25F49DB7AD7872DA5F846
```

The deployed bytes match the existing local bundle exactly. The resource is also present in the existing WAR at:

```text
WEB-INF/classes/META-INF/VAADIN/webapp/VAADIN/build/vaadin-custom-field-24463f6f.js
```

The entire file was inspected, not just its visible import statements. No actual Windows/Unix filesystem location, local username/home directory, Maven cache location, Java source path, server installation path, or concrete server stack trace was found in this response. AppScan did not supply its matched substring, so the exact scanner trigger cannot honestly be identified. The candidates below are the probable matches, not a claim about AppScan's undisclosed rule.

## Exact candidate text and classification

| Text in the response | Interpretation |
| --- | --- |
| `from"./generated-flow-imports-9e5fbc4d.js"` | JavaScript relative import of a public frontend bundle. |
| `from"./indexhtml-7cb6522f.js"` | JavaScript relative import of the public frontend entry bundle. |
| `hour23h:/^(2[0-3]|[0-1]?\d)/` | Date-parser property followed by a regular-expression literal; the substring `h:/` can resemble a drive prefix to a broad pattern. |
| `hour24h:/^(2[0-4]|[0-1]?\d)/` | Same regular-expression false positive. |
| `hour11h:/^(1[0-1]|0?\d)/` | Same regular-expression false positive. |
| `hour12h:/^(1[0-2]|0?\d)/` | Same regular-expression false positive. |
| `h.item.getAttribute("src").startsWith("file://")` | Client-side check of the URI scheme of a pasted image; there is no filename, hostname, drive, or directory after the scheme. |
| `[part='overlay']`, `:host([top-aligned]) [part~='overlay']` | CSS selectors embedded in a JavaScript template literal, not paths. |
| `/smis-test/VAADIN/build/...` | Public HTTP URL path, not an operating-system path. |

The four `h:/` candidates occur at zero-based decoded-text offsets 97,055, 97,084, 97,113 and 97,138; `file://` is at 977,458. Offsets are text indices, not byte offsets.

The surrounding `file://` code is:

```javascript
h.item.getAttribute("src").startsWith("file://")&&d.push(h.item)
```

The dependency's original source map identifies the source as:

```text
webpack://CKEDITOR/./node_modules/@ckeditor/ckeditor5-paste-from-office/src/filters/image.js
```

Its source checks pasted `<img>` elements and collects images whose `src` starts with `file://`. This is client clipboard processing, not disclosure of the server's filesystem. The bundle is a shared chunk containing other components and CKEditor; its filename does not establish a CustomField vulnerability.

## Versions, production profile and clean build

- Vaadin platform, Flow, Spring integration and CustomField: **24.2.0**.
- Spring Boot: **3.1.5**; Java release: **17**; packaging: **WAR**.
- The `production` Maven profile sets `vaadin.productionMode=true`, retains Vaadin CSRF protection, adds `vaadin:build-frontend` to `compile`, and excludes `vaadin-dev` through the profile's `vaadin-core` dependency.
- The actual build log confirms Java compilation precedes `prepare-frontend` and `build-frontend`, then Vite, tests, WAR creation and Spring Boot repackaging.
- No deployment script or confirmed historical deployment command was available. The correct command derived from this POM is:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
.\mvnw.cmd -Pproduction clean package
```

The build ran in an isolated copy of the current working files; existing generated files and the existing artifact were not edited. An initial npm install failed under offline sandbox restrictions; the authorized network-enabled retry passed with the same command.

**Build: SUCCESS. Tests: 11 run, zero failures, zero errors, zero skipped.** This includes four profile tests, two authentication-flow tests and five security-header tests. No new application test was needed because no application fix was made.

The new WAR is 104,798,878 bytes, SHA-256:

```text
46E1E9DFD099A64BDC39EB1AC05A29DC5DB3637CCDA0DB3199A06F306C544ED5
```

Its corresponding chunk is `vaadin-custom-field-6dc53c62.js` at the same archive directory. It differs from the deployed chunk only in the two imported bundle hashes; normalizing those two hashes makes the complete files identical. It has the same five candidate matches and no `sourceMappingURL`.

The new `META-INF/VAADIN/config/flow-build-info.json` contains:

```json
{"productionMode":true,"eagerServerLoad":false}
```

No `vaadin-dev`, `vaadin-dev-server`, `vaadin-dev-bundle`, or Spring Boot DevTools JAR is in the production WAR. Its public Vaadin output contains 24 JavaScript files, one CSS file and one HTML file, with **zero `.map` files and zero `sourceMappingURL` directives**. All 26 Brotli files were decompressed and verified to match their inspected uncompressed counterparts.

The existing local WAR was from a default development build: its token has `productionMode:false` and local build folders. Do not deploy that artifact. That does **not** establish the deployed server's mode: the live login bootstrap request `/?v-r=init&location=login` returned **`"productionMode":true`**, independently confirming the scanned deployment's runtime mode.

## HTTP verification

Checks used the real server and context path, without following redirects:

| Request, relative to `/smis-test/` | Result |
| --- | --- |
| GET affected `.js` | 200; exact matching JavaScript bytes |
| HEAD affected `.js.map` | **404** |
| GET affected `.js.map` | **404**, empty body |
| HEAD `VAADIN/build/ckeditor.js.map` | **404** |
| HEAD `VAADIN/config/flow-build-info.json` | **404** |
| HEAD `VAADIN/config/bundle-size.html` | **404** |
| HEAD `VAADIN/@vite/client` | **404** |
| HEAD `VAADIN/dev-tools` | **404** |
| GET normal login bootstrap | 200; `productionMode:true`, no debug flag |
| HEAD/GET `frontend/ckeditor.js.map`, `frontend/Flow.js.map`, `frontend/index.js.map`, `frontend/print-js/dist/print.map` | 200 **HTML bootstrap fallback**, not source maps; verified response body and `Content-Type: text/html` |

Thus no source map was accessible at the tested URLs. Do not interpret the fallback HTML's HTTP 200 as a map exposure. No detailed server trace or local path was observed in the checked responses. This is not a claim to have induced every possible application error; no deliberate live-server 500 was triggered.

The requested localhost check was also attempted: port 8080 had no listener. The direct deployed checks above provide the HTTP evidence instead.

```bash
curl -I http://10.179.2.248:8081/smis-test/VAADIN/build/vaadin-custom-field-24463f6f.js
curl -I http://10.179.2.248:8081/smis-test/VAADIN/build/vaadin-custom-field-24463f6f.js.map
curl -sS -D - -o /dev/null http://10.179.2.248:8081/smis-test/VAADIN/build/vaadin-custom-field-24463f6f.js.map
```

## Broader artifact inspection: private data versus public disclosure

Both generated frontend trees and both WARs were scanned, including textual resources inside nested dependency JARs. The existing scan inspected 1,517 text files; the production scan inspected 1,289. Each pattern occurrence, its location and surrounding context is retained in the linked evidence ledgers below. Counts include repeated copies and overlapping patterns; they are not counts of vulnerabilities.

| Other matches | Classification and exposure |
| --- | --- |
| `F:/Eclipse Workspace/SMIS/...` in the old `bundle-size.html`; `C:/Users/Aiban/.../path-disclosure/build/...` in the rebuilt one | Real build-machine paths in a private Vite visualization at `WEB-INF/classes/META-INF/VAADIN/config/`. This is outside `META-INF/VAADIN/webapp`; the tested public URL returned 404. Not present in the JavaScript response. |
| Old `flow-build-info.json` contains `npmFolder`, `frontendFolder`, `connect.javaSourceFolder` and other absolute paths | Private development-build metadata. The clean production token has no such fields. Its tested public URL returned 404. |
| `ckeditor.js.map`, `Flow.js.map`, `index.js.map` and associated `sourceMappingURL` comments in generated inputs and nested JAR `META-INF/frontend` resources | Dependency build inputs; absent from the public production output. CKEditor has `webpack://CKEDITOR/./node_modules/...` module identifiers; Flow has `../../../../src/main/frontend/Flow.ts` and `index.ts`, which are relative package source paths. HTTP checks did not retrieve these maps. |
| `B:\n`, `Client A:\n`, `Client B:\n` in CKEditor's map JSON | Prose followed by an escaped newline, not Windows drives. |
| `file://` and dynamically constructed source-map comments in CKEditor source-map content | Client-side library source, including the same pasted-image scheme check. No concrete server file URL. |
| `//# sourceMappingURL=print.map` in `vcf-pdf-viewer`'s `META-INF/resources/frontend/print-js/dist/print.js` | Upstream library reference; `print.map` is not packaged. Its tested URL serves HTML fallback, not a map. |
| `D:\a\commons-io\commons-io\target\...` in commons-io's nested POM comment | Upstream CI path in a private dependency POM, not this application's server location. |
| `C:\Documents and Settings\sony\...\jfreechart-cvs\`, `C:\model.xml` in jcommon's generator properties | Old upstream developer/example paths in a private dependency resource, not public JS. |
| `C:\dev\git\ph-commons\...`, `C:\dev\git\ph-css\...`, `C:\tools\apache-maven-3.9.3\...`, `C:\Users\phili\...` in ph-commons/ph-css buildinfo | Real upstream build metadata inside private dependency JARs, not this server's exposed assets. Every occurrence is retained in the ledger. No `.m2/repository` match was found. |
| `${project.basedir}/../etc/...`, `src/etc/...`, `/var/log`, `/etc/passwd` | Private dependency POM paths, documentation examples or Tomcat message templates. No concrete server error response. |
| `org/glassfish/...`, `org/apache/catalina/...` | Java package/resource patterns in dependency build metadata, not server installation paths. |
| `Caused by: ` in the old development bundle | Client exception-formatting code, not an actual server trace; the dev bundle is excluded from the production WAR. |

Production properties contain no source-map override or error-detail inclusion setting. Live-reload/restart settings in `application.properties` and JDWP arguments in `spring-boot:run` are development settings; the packaged production artifact excludes DevTools, and the Maven run arguments are not WAR deployment JVM arguments. Use the production packaging command; merely activating a Spring runtime profile does not run the Maven frontend build.

## Relevant security advisories

No matching CustomField path-disclosure/source-map advisory was identified in the [official Vaadin security reports](https://vaadin.com/security). The reported expressions are expected frontend code, so no dependency was upgraded to silence AppScan.

- [CVE-2020-36321](https://vaadin.com/security/cve-2020-36321) concerns older Vaadin development-mode directory traversal, not Vaadin 24.2.0 running in production.
- [CVE-2023-25499](https://vaadin.com/security/cve-2023-25499) concerns non-visible component data; 24.2.0 is outside the listed affected range.
- [CVE-2026-7860](https://vaadin.com/security/cve-2026-7860) **does include this old build-plugin version**, but concerns environment variables in failed frontend-build logs, not paths in served bundles. Track that as a separate dependency-maintenance issue; this investigation is not a clean security bill for all dependencies.

## Evidence files and changes

Only this report was added to the project. No production source, POM, dependency, generated file, JavaScript response, route or security filter was changed.

- [Existing artifact scan: every match with context](</C:/Users/Aiban/.codex/visualizations/2026/09/08/01a07f87-4652-7bb2-8230-e77be4d4bcfc/path-disclosure/existing-matches.jsonl>)
- [Production artifact scan: every match with context](</C:/Users/Aiban/.codex/visualizations/2026/09/08/01a07f87-4652-7bb2-8230-e77be4d4bcfc/path-disclosure/production-matches.jsonl>)
- [Read-only artifact scanning script](</C:/Users/Aiban/.codex/visualizations/2026/09/08/01a07f87-4652-7bb2-8230-e77be4d4bcfc/path-disclosure/scan.ps1>)
- [Production build and test log](</C:/Users/Aiban/.codex/visualizations/2026/09/08/01a07f87-4652-7bb2-8230-e77be4d4bcfc/path-disclosure/production-build.log>)
- [Built production WAR](</C:/Users/Aiban/.codex/visualizations/2026/09/08/01a07f87-4652-7bb2-8230-e77be4d4bcfc/path-disclosure/build/target/smis-2.0.war>)

## Suggested AppScan closure justification

> Likely false positive. The complete deployed JavaScript was retrieved and matched by SHA-256 to the inspected packaged resource. No actual operating-system filesystem path or server stack trace was present. Candidate patterns are relative frontend imports, date-parser regular expressions and a client-side `file://` scheme check in CKEditor pasted-image processing. The response contains no sourceMappingURL, and both GET and HEAD of its source-map URL return 404. The deployed bootstrap confirms productionMode=true. AppScan supplied no matched substring, so its exact heuristic trigger remains unconfirmed. No response rewriting, resource blocking or generated-code modification is warranted.
