# Stored rich-text XSS protection

Verified 8 September 2026 against Spring Boot 3.1.5, Vaadin 24.2.0, Java 17 and `com.wontlost:ckeditor-vaadin:4.0.0`.

## Root cause and inventory

The release-order editor previously accepted raw client HTML, saved it unchanged and loaded legacy HTML directly into CKEditor. Authentication did not make this HTML trustworthy. The same HTML was passed to Jasper's HTML markup renderer. Generated note templates also concatenated unescaped database metadata.

| Field / entry point | Storage / rendering | Protection |
| --- | --- | --- |
| `PrintView.inlineEditor`, authenticated `releaseordermla` route | `Installment.copyTo`, `installment.copy_to`, VARCHAR(2000), key `installment_id` | Safe editor wrapper, service sanitization, JPA converter |
| `InstallmentNew.copyTo` | `installment_new.copy_to`, declared length 2000; no active editor population found | `NewService.saveInstallment` and JPA converter |
| `PrintView.populateEditor` | Existing notes and generated templates sent to CKEditor's `editorData` / `updateData` JavaScript sink | Sanitize before sending; escape plain metadata before composing HTML |
| `PrintView.printReport` | Jasper parameter `copyTo` | One sanitized value is validated before saving and reused for the report |
| `Work.installments` | Cascade ALL | `Dbservice.saveWork` prepares initialized children; converter protects writes during merge/flush |
| `PrintViewMp.copyTo` | Plain TextField, transient report parameter `CopyTo` | Remains plain text; no rich storage field on `Installmentmp` |

HTML Jasper sinks are `src/main/resources/report/Release11`, `12`, `13`, `21`, `22`, `23`, `31`, `32`, `33`, `41`, `42`, `43` (`.jrxml`). `Release11 - Copy.jrxml` also declares the parameter but is not selected by the active report path. The MP report `CopyTo` text fields have no HTML markup mode. Report templates were not changed.

Inspected all application Java sources for editor constructors, setters/getters, repository writes, REST/controllers, native/bulk updates, `innerHTML`, `new Html`, element properties, JavaScript calls, previews and template composition. No additional active rich-text editor, public HTML renderer, REST HTML input, HTML import, draft/publish operation, or native bulk writer of these columns was found. The only other `executeJs` search result is commented code in `LoginOld`. Vaadin's generated/client dependency files are not treated as application write boundaries.

Known write callers include release-order printing, `WorkForm` / `WorkView`, and `processflow/WorkFormNew`. All application rich-field writes converge on `Dbservice.saveInstallment`, `Dbservice.saveWork`, or `NewService.saveInstallment`. Explicit JPA converters additionally protect repository `save`/`saveAll`, cascades and dirty checking. Native SQL outside these entities can bypass converters; no such application writer was found. The maintenance utility deliberately uses JDBC with the same sanitizer.

## Central policy

Dependency: `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20260313.1`, verified in the [official OWASP releases](https://github.com/OWASP/java-html-sanitizer/releases). This stable release is compatible with Java 17. No framework/editor upgrade was introduced.

`RichTextHtmlSanitizer` owns one static immutable `PolicyFactory`. It uses OWASP's HTML parser and CSS validator, not regex or string replacement.

* Elements: `p br div h1 h2 h3 h4 h5 h6 strong b em i u s sub sup ul ol li blockquote pre code table thead tbody tfoot tr th td a img span figure figcaption hr`.
* Attributes: `a[href,title]`; `img[src,alt,title,width,height]`; `th/td[colspan,rowspan]`; `ol[start]`; `figure[class]`; validated `style` on allowed elements.
* Image dimensions: integers 1–2000; spans: integers 1–100; ordered-list start: integers −10000–10000.
* Figure class: exactly `table` or `image`; all other classes and IDs removed.
* Links: HTTP, HTTPS, mailto, tel and normal relative references. No target or ping; links cannot request a new browsing context. Unsupported schemes are removed.
* Images: exact existing application resources `/images/plant.png`, `/images/empty-plant.png`, `images/plant.png`, `images/empty-plant.png`. This is an exact resource allowlist, not a directory prefix. No upload/media endpoint was found. External hosts, protocol-relative images, Base64/data, SVG, file and blob images are disallowed, as confirmed by the user. No stored images were found in the examined rows.
* CSS properties: `font-family`, `font-size`, `font-weight`, `font-style`, `text-decoration`, `text-align`, `color`, `background-color`, `line-height`. Values use OWASP's built-in safe grammar, including RGB/RGBA/HSL/HSLA color functions. No URL-bearing CSS, expressions, positioning, arbitrary CSS properties, or stylesheets. The function definitions must be included explicitly in a restricted [OWASP CSS schema](https://github.com/OWASP/java-html-sanitizer/blob/main/owasp-java-html-sanitizer/src/main/java/org/owasp/html/CssSchema.java).
* Everything else is stripped, including scripts, frames, embeds, SVG/MathML, forms, controls, metadata, `srcdoc` and event handlers.

Null remains null for persistence; blank becomes empty. Raw input over 100,000 Java characters is rejected before parsing. Sanitized storage over the existing 2,000-character column limit is rejected, never truncated. Oversized legacy input is withheld from rendering; legacy content within the input bound is sanitized before display even if its normalized form exceeds the storage limit. Length-only log events describe normalization, not an assumed attack, and contain no HTML.

## Editor and rendering behavior

`SafeRichTextEditor` encapsulates the raw editor. `setValue` sanitizes legacy/template HTML before the browser sink. `getValue` sanitizes and checks storage length before either persistence or Jasper use. `PrintView` HTML-escapes database metadata before composing templates; nothing untrusted is appended after the final sanitization.

The toolbar retains headings, fonts, sizes, colors, bold/italic/underline/strike, sub/superscripts, formatting removal, links, lists, alignment, quotes, code, tables, horizontal rules, special characters and undo/redo. Font choices are default, Times New Roman/Times/serif and Arial/Helvetica/sans-serif; size choices are 10, 12, 14, 16, 18 and 24.

Source Editing, HTML Embed, General HTML Support, media embedding, image insertion/upload/adapters, EasyImage, image resizing, arbitrary table/cell properties, todo lists, page breaks and block indentation are disabled. Existing default plugin exclusions are preserved. GHS and autosave are explicitly false. EasyImage is removed together with its ImageUpload dependency. The dependency's plugin requirements were inspected to avoid leaving an enabled plugin dependent on one removed here.

The browser configuration is for usability; server-side sanitization is the security control. Existing authentication, authorization, CSP, security headers, CSRF, CORS, Vaadin routes and push configuration were not changed by this work.

## Existing-data dry run

A standalone JDBC read-only run against the repository's configured **local** `localhost:5432/smisold` database examined 12,621 rows:

| Result | Count |
| --- | ---: |
| Sanitizable changed rows | 1,057 |
| Blocked rows (normalized content exceeds storage limit) | 20 |
| Unchanged rows | 11,544 |
| Database updates | 0 |
| Images / data images / external images / iframes in successfully checked rows | 0 / 0 / 0 / 0 |

`installment.copy_to` capacity is 2000. `installment_new.copy_to` was absent and explicitly skipped. These counts describe the local database, not the scanned deployment database. Changed records include harmless parser/CSS/entity normalization; the count is not an attack count. The tool returns a nonzero exit code when blocked records require review.

See [record IDs and summary](stored-xss-dry-run.txt). No row HTML is included. The 20 blocked records must be reviewed and deliberately shortened, or a separately approved schema/limit change made, before apply is possible. No automatic cleanup was performed.

## One-time cleanup procedure

1. Deploy the rendering/write protections first. Reproduce the dry run against a staging copy of the actual deployment database. Stop application writers during the reviewed final dry run and apply; the scan is batched, not a whole-database snapshot.
2. Create and test a restorable database backup with your normal DBA procedure. The utility checks that the supplied backup file exists and is nonempty and requires explicit confirmation; it cannot prove that the file is a valid backup of the selected database.
3. Prepare a protected Java properties file containing literal resolved `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` for the intended database. Environment placeholders and Spring profile imports are not evaluated by this standalone utility. Do not put passwords in command-line arguments or commit this file.
4. Extract the production WAR into a new maintenance directory (using JDK 17), then dry-run:

```powershell
# From a new, empty maintenance directory; substitute the actual WAR and config paths.
jar xf C:\releases\smis-2.0.war WEB-INF/classes WEB-INF/lib
java -cp 'WEB-INF/classes;WEB-INF/lib/*' `
  com.smis.security.richtext.RichTextMaintenance `
  --properties C:\secure\maintenance.properties > dry-run.log
```

5. Review all IDs and blocked counts. After resolving blocked records, obtain a fresh dry run and backup. Only then explicitly apply with the reviewed changed count:

```powershell
java -cp 'WEB-INF/classes;WEB-INF/lib/*' `
  com.smis.security.richtext.RichTextMaintenance `
  --properties C:\secure\maintenance.properties `
  --apply --backup C:\backups\smis-before-richtext.dump --confirm-backup `
  --expected-changed <reviewed-count> > apply.log
```

6. Repeat dry run: expect changed=0 and blocked=0. Review representative release orders and PDF output before reopening writers. Retain render-time sanitization afterward.

Apply performs a fresh preflight, rejects blocked rows or an unexpected changed count, and commits at most 200 scanned rows per transaction. Compare-and-set updates detect concurrent changes. A failure rolls back the current batch; earlier committed batches remain, so resume with a new dry run/count. Repeated successful application is idempotent. The tool is not a Spring startup runner or an HTTP endpoint and does not start the application or execute its SQL initialization.

## Files changed for this request

* `pom.xml`: sanitizer dependency and test-only H2.
* `src/main/java/com/smis/security/richtext/`: new `RichTextHtmlSanitizer`, `RichTextHtmlConverter`, `RichTextContentService`, `SafeRichTextEditor`, `RichTextMaintenance`.
* `src/main/java/com/smis/entity/Installment.java`, `InstallmentNew.java`: explicit converters.
* `src/main/java/com/smis/dbservice/Dbservice.java`, `NewService.java`: transactional service preparation; relevant save failures now propagate instead of being swallowed.
* `src/main/java/com/smis/view/PrintView.java`: shared wrapper, safe report parameter, escaped metadata, payload-free report failure handling.
* `src/test/java/com/smis/security/richtext/`: four regression test classes.
* This report and `stored-xss-dry-run.txt`.

Earlier Referrer-Policy/no-auth/AppScan changes already present in the working tree were preserved. No generated or minified file was manually edited.

## Verification results

The complete suite and production package passed using JDK 17 and `.\mvnw.cmd -Pproduction clean package`. An isolated copy was built so Vaadin could regenerate its frontend without disturbing tracked generated files in the working tree. The sandboxed npm attempt failed on an uncached dependency; the authorized retry with network/cache access succeeded.

| Tests | Run | Result |
| --- | ---: | --- |
| `RichTextHtmlSanitizerTest` | 32 | Passed: requested attack categories, encoded protocols, malformed/nested markup, CSS, image restrictions, legitimate formatting, size bounds, idempotency |
| `RichTextPersistenceTest` | 4 | Passed against H2 using real Hibernate entities/converters and Spring Data repositories: service create/edit bypassing UI, process service, repository saveAll, cascade, dirty checking and size rejection |
| `SafeRichTextEditorTest` | 2 | Passed: browser-bound legacy HTML and untrusted editor output are sanitized |
| `RichTextMaintenanceTest` | 1 | Passed: dry-run leaves raw fixture unchanged, apply sanitizes, reapply changes nothing, output excludes payload |
| Existing security tests | 11 | Passed, including all 5 Referrer-Policy tests |
| **Total** | **50** | **No failures, errors or skipped tests** |

Built WAR: `smis-2.0.war`. Verified `WEB-INF/lib/owasp-java-html-sanitizer-20260313.1.jar` and sanitizer classes are packaged. `WEB-INF/classes/META-INF/VAADIN/config/flow-build-info.json` contains `productionMode: true`. No vaadin-dev or Spring Boot devtools JAR was packaged. No top-level frontend `.map` entry was introduced. This last check does not claim that third-party nested JARs contain no source maps; see the separate AppScan investigation.

View regression tests exercise the server-side Vaadin element boundary. A separate headless Edge attempt did not produce a usable browser result on this host, and the browser tool blocked the local test page under its URL policy. No browser-policy workaround was attempted. Actual interactive editor behavior and Jasper PDF layout still need the normal deployment acceptance check. No live application/database cleanup was performed.

## Compatibility and remaining scope

Unsupported content includes embedded/external images, frames/media, arbitrary HTML/CSS/classes, source editing, arbitrary table decoration, page breaks and content beyond the existing storage limit. Normalization may change markup serialization, font casing and entities while retaining supported formatting. The 20 oversized local rows require explicit content/schema review. Approved images are fixed existing resources; adding upload paths later requires review of storage, serving and the central allowlist.

The controls cover the discovered rich-field write and rendering paths. They do not claim to fix unrelated application vulnerabilities, arbitrary future native SQL writers, compromised dependencies, or new renderers that bypass the shared boundary. Production database counts and interactive acceptance remain deployment work; no production access or cleanup is implied by this local verification.
