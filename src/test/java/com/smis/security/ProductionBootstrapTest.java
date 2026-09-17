package com.smis.security;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Run after package: -Dtest=ProductionBootstrapTest -Dsmis.production-test=true surefire:test. */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="smis.production-test", matches="true")
@SpringBootTest(classes=com.smis.SmisJuneApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"vaadin.productionMode=true", "spring.datasource.url=jdbc:h2:mem:bootstrap;MODE=PostgreSQL;NON_KEYWORDS=YEAR",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "server.port=0"})
class ProductionBootstrapTest {
    @LocalServerPort int port;

    @Test void actualProductionBootstrapHasSinglePolicyAndFreshMatchingNonce() throws Exception {
        var client = HttpClient.newHttpClient();
        String previous = null;
        for (int i=0; i<2; i++) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/login")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            var policies = response.headers().allValues("Content-Security-Policy");
            assertThat(policies).hasSize(1);
            var scripts = Jsoup.parse(response.body()).select("script");
            assertThat(scripts).isNotEmpty();
            assertThat(scripts).anyMatch(script -> script.attr("src").matches("\\./security/csp-compat\\.js\\?v=[a-f0-9]{64}"));
            String nonce = scripts.first().attr("nonce");
            assertThat(nonce).matches("[A-Za-z0-9+/]{43}=").isNotEqualTo(previous);
            assertThat(scripts).allMatch(script -> nonce.equals(script.attr("nonce")));
            assertThat(policies).containsExactly(SecurityHeadersPolicy.forBootstrap(nonce));
            assertThat(policies.get(0)).doesNotContain("'unsafe-eval'").contains("require-trusted-types-for 'script'");
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            previous = nonce;
        }
    }

    @Test void compatibilityScriptsArePublicJavaScriptNotLoginRedirects() throws Exception {
        var client = HttpClient.newHttpClient();
        for (String name : new String[]{"purify.min.js", "csp-registry.js", "csp-compat.js"}) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/security/"+name)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("javascript");
            assertThat(response.body()).doesNotContain("<!doctype html>");
        }
    }

    @Test void actualUidlRejectsInvalidIdsAndSameSessionStillProcessesValidRpc() throws Exception {
        var client = HttpClient.newHttpClient();
        String base = "http://localhost:" + port;
        var bootstrap = client.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        // Production cookies stay Secure. Explicitly forward only this isolated test cookie on loopback.
        String cookie = bootstrap.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        var initialResponse = client.send(HttpRequest.newBuilder(URI.create(base + "/?v-r=init&location=login&query="))
                .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var initial = mapper.readTree(initialResponse.body()).path("appConfig");
        var uidl = initial.path("uidl");
        String prefix = "{\"csrfToken\":" + mapper.writeValueAsString(uidl.path("Vaadin-Security-Key").asText())
                + ",\"rpc\":[],\"syncId\":" + uidl.path("syncId").asInt() + ",\"clientId\":";
        URI endpoint = URI.create(base + "/?v-r=uidl&v-uiId=" + initial.path("v-uiId").asInt());
        for (String id : new String[]{"2147483648", "4294967297", "-2147483649", "9999999999999999999999999", "1.5", "\"1\"", "null"}) {
            var response = client.send(HttpRequest.newBuilder(endpoint).header("Cookie", cookie)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(prefix + id + "}")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("Invalid request");
        }
        var valid = client.send(HttpRequest.newBuilder(endpoint).header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(prefix + uidl.path("clientId").asInt() + "}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(valid.statusCode()).isEqualTo(200);
        assertThat(valid.body()).startsWith("for(;;);[").doesNotContain("appError");
    }
}
