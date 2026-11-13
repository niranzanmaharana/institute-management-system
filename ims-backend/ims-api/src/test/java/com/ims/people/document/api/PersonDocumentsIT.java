package com.ims.people.document.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PersonDocumentsIT {

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
    registry.add("ims.storage.type", () -> "memory");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void oversizedRejectedAndCrossTenantDownloadDenied() throws Exception {
    String tokenA = login("admin", "Password@123", "DEMO_A");
    String tokenB = login("admin", "Password@123", "DEMO_B");
    String suffix = String.valueOf(System.currentTimeMillis());

    long studentA =
        postJson(
                tokenA,
                "/api/v1/students",
                "{\"firstName\":\"Doc\",\"lastName\":\"A\",\"studentCode\":\"DOC_"
                    + suffix
                    + "\",\"guardians\":[],\"addresses\":[]}",
                201)
            .path("id")
            .asLong();

    String docsBase = "/api/v1/people/STUDENT/" + studentA + "/documents";

    mockMvc
        .perform(
            post(docsBase + "/uploads")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "docType":"ID_PROOF",
                      "fileName":"huge.pdf",
                      "contentType":"application/pdf",
                      "fileSize":6000000
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("5 MB")));

    JsonNode init =
        postJson(
            tokenA,
            docsBase + "/uploads",
            """
            {
              "docType":"ID_PROOF",
              "fileName":"id.pdf",
              "contentType":"application/pdf",
              "fileSize":1024
            }
            """,
            201);
    long documentId = init.path("document").path("id").asLong();

    mockMvc
        .perform(
            post(docsBase + "/" + documentId + "/complete")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"checksum\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(get(docsBase).header("Authorization", "Bearer " + tokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(documentId));

    mockMvc
        .perform(
            get(docsBase + "/" + documentId + "/download")
                .header("Authorization", "Bearer " + tokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.downloadUrl").isNotEmpty());

    mockMvc
        .perform(
            get("/api/v1/people/STUDENT/" + studentA + "/documents/" + documentId + "/download")
                .header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/v1/people/STUDENT/" + studentA + "/documents")
                .header("Authorization", "Bearer " + tokenB))
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
