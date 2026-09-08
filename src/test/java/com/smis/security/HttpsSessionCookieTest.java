package com.smis.security;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.*;
import java.nio.file.*;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = HttpsSessionCookieTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpsSessionCookieTest {
    private static final char[] PASSWORD = "test-only-password".toCharArray();
    private static Path keyStore;
    @LocalServerPort int port;

    @DynamicPropertySource
    static void https(DynamicPropertyRegistry properties) throws Exception {
        keyStore = Files.createTempDirectory("smis-https-test-").resolve("test.p12");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "localhost", "-keyalg", "RSA", "-keysize", "2048",
                "-storetype", "PKCS12", "-keystore", keyStore.toString(), "-storepass", new String(PASSWORD),
                "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-validity", "2", "-noprompt")
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor()).as(output).isZero();
        keyStore.toFile().deleteOnExit();
        keyStore.getParent().toFile().deleteOnExit();
        properties.add("server.ssl.enabled", () -> true);
        properties.add("server.ssl.key-store", () -> keyStore.toUri().toString());
        properties.add("server.ssl.key-store-password", () -> new String(PASSWORD));
        properties.add("server.ssl.key-store-type", () -> "PKCS12");
        properties.add("server.servlet.context-path", () -> "/smis-test");
    }

    @Test
    void realHttpsSessionLoginAndLogoutUseTheActualSecureCookie() throws Exception {
        var store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(keyStore)) { store.load(input, PASSWORD); }
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        var ssl = SSLContext.getInstance("TLS");
        ssl.init(null, trust.getTrustManagers(), null);
        var client = HttpClient.newBuilder().sslContext(ssl)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String base = "https://localhost:" + port + "/smis-test";
        var session = client.send(HttpRequest.newBuilder(URI.create(base + "/images/session.png")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(session.statusCode()).isEqualTo(200);
        assertThat(session.headers().allValues("Set-Cookie")).singleElement().satisfies(cookie ->
                assertThat(cookie).startsWith("JSESSIONID=").contains("Path=/smis-test", "Secure", "HttpOnly", "SameSite=Strict"));
        assertThat(session.headers().allValues("Strict-Transport-Security")).containsExactly("max-age=31536000");
        var login = client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=tester&password=unused&_csrf="
                        + java.net.URLEncoder.encode(session.body(), java.nio.charset.StandardCharsets.UTF_8))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isBetween(300, 399);
        assertThat(login.headers().firstValue("Location").orElse("")).doesNotContain("error");
        var authenticated = client.send(HttpRequest.newBuilder(URI.create(base + "/api/header-probe")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(authenticated.statusCode()).isEqualTo(200);
        assertThat(authenticated.headers().allValues("Content-Security-Policy")).containsExactly(SecurityHeadersPolicy.CSP);
        var refreshed = client.send(HttpRequest.newBuilder(URI.create(base + "/images/session.png")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        var logout = client.send(HttpRequest.newBuilder(URI.create(base + "/logout"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("_csrf=" + java.net.URLEncoder.encode(refreshed.body(),
                        java.nio.charset.StandardCharsets.UTF_8))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(logout.statusCode()).isBetween(300, 399);
        var afterLogout = client.send(HttpRequest.newBuilder(URI.create(base + "/api/header-probe")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(afterLogout.statusCode()).isBetween(300, 399);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({ReferrerPolicyTest.Application.class, SessionCookieConfiguration.class})
    static class Application {}
}
