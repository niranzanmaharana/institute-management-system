package com.ims.platform.tenancy.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Phase 2.8 — cross-tenant isolation for student, course, enrollment-related resources.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SliceIsolationIT {

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
  void tokenBCannotReadTenantAStudentOrCourse() throws Exception {
    String tokenA = login("admin", "Password@123", "DEMO_A");
    String tokenB = login("admin", "Password@123", "DEMO_B");
    String suffix = String.valueOf(System.currentTimeMillis());

    long studentA =
        postJson(
                tokenA,
                "/api/v1/students",
                "{\"studentCode\":\"ISO_"
                    + suffix
                    + "\",\"firstName\":\"Iso\",\"lastName\":\"A\",\"guardians\":[],\"addresses\":[]}",
                201)
            .path("id")
            .asLong();

    long courseA =
        postJson(
                tokenA,
                "/api/v1/courses",
                "{\"code\":\"ISO_C_" + suffix + "\",\"name\":\"Iso Course\"}",
                201)
            .path("id")
            .asLong();

    // Same code allowed in other tenant
    postJson(
        tokenB,
        "/api/v1/courses",
        "{\"code\":\"ISO_C_" + suffix + "\",\"name\":\"Iso Course B\"}",
        201);

    mockMvc
        .perform(get("/api/v1/students/" + studentA).header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/courses/" + courseA).header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isNotFound());
  }

  private JsonNode postJson(String token, String path, String body, int expected) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(path)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(expected))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String login(String username, String password, String instituteCode) throws Exception {
    String body =
        "{\"username\":\""
            + username
            + "\",\"password\":\""
            + password
            + "\",\"instituteCode\":\""
            + instituteCode
            + "\"}";
    MvcResult result =
        mockMvc
            .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
  }
}
