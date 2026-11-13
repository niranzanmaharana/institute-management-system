package com.ims.platform.institute.api;

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
class InstituteControllerIT {

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
  void platformAdminCanCreateSuspendAndList() throws Exception {
    String token = loginAccessToken("platform", "Password@123", null);

    mockMvc
        .perform(get("/api/v1/institutes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").exists());

    String code = "IT_" + System.currentTimeMillis();
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/institutes")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "code":"%s",
                          "name":"Integration Test Institute",
                          "timezone":"Asia/Kolkata",
                          "profile":{
                            "mobile":"9876543210",
                            "adminEmail":"contact@example.com",
                            "website":"https://example.com",
                            "addressLine1":"1 Test Road",
                            "city":"Pune",
                            "state":"MH",
                            "postalCode":"411001",
                            "country":"India",
                            "iconUrl":"https://placehold.co/64x64/png?text=IT"
                          },
                          "initialAdmin":{
                            "username":"itadmin",
                            "email":"itadmin@example.com",
                            "password":"Password@123"
                          }
                        }
                        """
                            .formatted(code)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.mobile").value("9876543210"))
            .andExpect(jsonPath("$.adminEmail").value("contact@example.com"))
            .andExpect(jsonPath("$.city").value("Pune"))
            .andReturn();

    long id =
        objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"itadmin","password":"Password@123","instituteCode":"%s"}
                    """
                        .formatted(code)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.roles", org.hamcrest.Matchers.hasItem("ADMIN")));

    mockMvc
        .perform(
            post("/api/v1/institutes/" + id + "/suspend")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUSPENDED"));
  }

  @Test
  void instituteAdminCanListOwnButCannotCreate() throws Exception {
    String token = loginAccessToken("admin", "Password@123", "DEMO_A");

    mockMvc
        .perform(
            post("/api/v1/institutes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"NOPE","name":"Should Fail","timezone":"Asia/Kolkata"}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/v1/institutes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].code").value("DEMO_A"));
  }

  private String loginAccessToken(String username, String password, String instituteCode)
      throws Exception {
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
