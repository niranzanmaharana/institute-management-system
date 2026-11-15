package com.ims.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Step 7.5 — committed OpenAPI contract must match springdoc output.
 *
 * <p>Refresh with {@code scripts/openapi/export-openapi.ps1} after intentional API changes. Breaking
 * changes also require bumping {@code info.version} in {@link OpenApiConfig} (see docs/api).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OpenApiContractIT {

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("ims")
          .withUsername("ims")
          .withPassword("ims");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("ims.security.jwt-secret", () -> "ims-test-jwt-secret-change-me-32chars-min!");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void committedOpenApiMatchesSpringdoc() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

    ObjectNode generated = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsByteArray());
    generated.remove("servers");

    Path committed = resolveCommittedContract();
    assertThat(committed).as("committed contract %s", committed).exists();
    ObjectNode expected = (ObjectNode) objectMapper.readTree(Files.readString(committed));
    expected.remove("servers");

    assertThat(generated)
        .as(
            "OpenAPI drift vs %s — run scripts/openapi/export-openapi.ps1 and commit docs/api/openapi.json"
                + " (bump OpenApiConfig info.version for breaking changes)",
            committed)
        .isEqualTo(expected);
  }

  private static Path resolveCommittedContract() {
    Path moduleDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    Path fromApiModule = moduleDir.resolve("../../docs/api/openapi.json").normalize();
    if (Files.isRegularFile(fromApiModule)) {
      return fromApiModule;
    }
    Path fromRepoRoot = moduleDir.resolve("docs/api/openapi.json").normalize();
    if (Files.isRegularFile(fromRepoRoot)) {
      return fromRepoRoot;
    }
    throw new IllegalStateException(
        "Cannot find docs/api/openapi.json from user.dir=" + moduleDir);
  }
}
