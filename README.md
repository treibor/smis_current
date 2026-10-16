# SMIS

## Tomcat deployment

Build the deployable WAR with JDK 17 using the production Maven profile:

```powershell
.\mvnw.cmd clean package -Pproduction
```

Deploy the WAR from `target` to Tomcat 10.1. The production build compiles the
Java views before generating the Vaadin JavaScript bundle and application theme.
Do not deploy an IDE-exported WAR or a development build.
When replacing an existing deployment, stop or undeploy that application first
and replace its old expanded application directory along with the WAR.
If the browser retains an older PWA bundle, clear site data for the deployment
and reload.

## Login

All application views require login. Enter the displayed CAPTCHA before signing in.
CAPTCHA is checked on the server before credentials and refreshed after each attempt.
The login username, password and CAPTCHA answer stay in browser-only fields and are encrypted with RSA-OAEP/SHA-256 before the login RPC is sent. HTTPS uses Web Crypto; HTTP staging uses bundled Forge with browser-generated cryptographic randomness. A per-view key and single-use OAEP challenge reject reuse across views or login attempts. There is no plaintext fallback. HTTP supports this payload encryption but does not protect the delivered page, public key, or session cookie against interception; use HTTPS for transport protection.
