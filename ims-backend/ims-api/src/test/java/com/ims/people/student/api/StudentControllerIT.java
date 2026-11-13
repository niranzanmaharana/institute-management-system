package com.ims.people.student.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class StudentControllerIT {

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
  void shouldCreateListGetUpdateAndDeactivateStudent() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/students")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "studentCode":"STU001",
                          "firstName":"Ada",
                          "lastName":"Lovelace",
                          "phone":"9000000001",
                          "email":"ada@example.com",
                          "guardians":[
                            {"name":"Lord Byron","phone":"9111111111","email":"byron@example.com","relation":"Father","primaryGuardian":true}
                          ],
                          "addresses":[
                            {"line1":"1 Analytical Engine Rd","city":"Pune","state":"MH","postalCode":"411001","country":"India","primaryAddress":true}
                          ]
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.studentCode").value("STU001"))
            .andExpect(jsonPath("$.guardians", hasSize(1)))
            .andExpect(jsonPath("$.addresses", hasSize(1)))
            .andReturn();

    long id =
        objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

    mockMvc
        .perform(get("/api/v1/students").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].studentCode").value("STU001"));

    mockMvc
        .perform(get("/api/v1/students/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Ada"));

    mockMvc
        .perform(
            put("/api/v1/students/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"firstName":"Augusta","lastName":"Lovelace","phone":"9000000001","email":"ada@example.com"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Augusta"))
        .andExpect(jsonPath("$.studentCode").value("STU001"));

    mockMvc
        .perform(
            post("/api/v1/students/" + id + "/deactivate")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));

    mockMvc
        .perform(get("/api/v1/students/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldShareGuardianAcrossStudents() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");

    mockMvc
        .perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "studentCode":"SIB1",
                      "firstName":"Alice",
                      "lastName":"Shared",
                      "guardians":[
                        {"name":"Parent Shared","phone":"9222222222","relation":"Mother","primaryGuardian":true}
                      ]
                    }
                    """))
        .andExpect(status().isCreated());

    MvcResult second =
        mockMvc
            .perform(
                post("/api/v1/students")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "studentCode":"SIB2",
                          "firstName":"Bob",
                          "lastName":"Shared",
                          "guardians":[
                            {"name":"Parent Shared","phone":"9222222222","relation":"Mother","primaryGuardian":true}
                          ]
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.guardians[0].phone").value("9222222222"))
            .andReturn();

    long guardianId =
        objectMapper
            .readTree(second.getResponse().getContentAsString())
            .path("guardians")
            .get(0)
            .path("id")
            .asLong();

    MvcResult first =
        mockMvc
            .perform(get("/api/v1/students?q=SIB1").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    long firstId =
        objectMapper
            .readTree(first.getResponse().getContentAsString())
            .path("content")
            .get(0)
            .path("id")
            .asLong();

    mockMvc
        .perform(get("/api/v1/students/" + firstId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.guardians[0].id").value(guardianId));
  }

  @Test
  void shouldIsolateStudentsAcrossTenants() throws Exception {
    String tokenA = login("admin", "Password@123", "DEMO_A");
    String tokenB = login("admin", "Password@123", "DEMO_B");

    MvcResult createdA =
        mockMvc
            .perform(
                post("/api/v1/students")
                    .header("Authorization", "Bearer " + tokenA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"studentCode":"SAME","firstName":"Tenant","lastName":"A"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    long idA =
        objectMapper.readTree(createdA.getResponse().getContentAsString()).path("id").asLong();

    mockMvc
        .perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"studentCode":"SAME","firstName":"Tenant","lastName":"B"}
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/students/" + idA).header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRejectDuplicateCodeWithinTenant() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String body =
        """
        {"studentCode":"DUP1","firstName":"One","lastName":"Student"}
        """;
    mockMvc
        .perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void platformAdminWithoutTenantCannotCreateStudent() throws Exception {
    String token = login("platform", "Password@123", null);
    mockMvc
        .perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"studentCode":"X","firstName":"No","lastName":"Tenant"}
                    """))
        .andExpect(status().isForbidden());
  }

  private String login(String username, String password, String instituteCode) throws Exception {
    String body =
        instituteCode == null
            ? "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"
            : "{\"username\":\""
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
    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    return json.path("accessToken").asText();
  }
}
