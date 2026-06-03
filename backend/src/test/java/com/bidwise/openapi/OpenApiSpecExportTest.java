package com.bidwise.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the app, scrapes the live OpenAPI spec from {@code /v3/api-docs}, and writes
 * it to {@code target/openapi.json}. This is the single source of truth consumed by
 * the frontend Orval codegen — running it as a test keeps the contract export in the
 * standard {@code mvn test}/{@code verify} lifecycle, with no running server or DB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiSpecExportTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exportsOpenApiSpec() throws Exception {
        String spec = restTemplate.getForObject("/v3/api-docs", String.class);

        assertThat(spec).contains("\"openapi\"");
        assertThat(spec).contains("/api/auth/register");
        assertThat(spec).contains("/api/auth/login");

        Path output = Path.of("target", "openapi.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, spec, StandardCharsets.UTF_8);
        assertThat(Files.exists(output)).isTrue();
    }
}
