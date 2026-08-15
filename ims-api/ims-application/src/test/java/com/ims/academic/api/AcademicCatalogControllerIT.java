package com.ims.academic.api;

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
class AcademicCatalogControllerIT {

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
  void createCourseFeePlanBatchAndRejectBadInstallmentSum() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());

    long categoryId =
        postJson(
                token,
                "/api/v1/fee-categories",
                "{\"code\":\"TUIT_" + suffix + "\",\"name\":\"Tuition\"}",
                201)
            .path("id")
            .asLong();

    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"AY_"
                    + suffix
                    + "\",\"name\":\"Year "
                    + suffix
                    + "\",\"startDate\":\"2026-04-01\",\"endDate\":\"2027-03-31\"}",
                201)
            .path("id")
            .asLong();

    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"CRS_" + suffix + "\",\"name\":\"Java Bootcamp\",\"description\":\"Demo\"}",
                201)
            .path("id")
            .asLong();

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/fee-plans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "code":"FP_BAD_%s",
                      "name":"Bad plan",
                      "currency":"INR",
                      "totalAmount":10000.00,
                      "installments":[
                        {"seq":1,"feeCategoryId":%d,"label":"A","amount":4000.00,"dueOffsetDays":0}
                      ]
                    }
                    """
                        .formatted(suffix, categoryId)))
        .andExpect(status().isBadRequest());

    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {
                  "code":"FP_OK_%s",
                  "name":"Default",
                  "currency":"INR",
                  "totalAmount":10000.00,
                  "installments":[
                    {"seq":1,"feeCategoryId":%d,"label":"Admission","amount":2000.00,"dueOffsetDays":0},
                    {"seq":2,"feeCategoryId":%d,"label":"Tuition","amount":8000.00,"dueOffsetDays":30}
                  ]
                }
                """
                    .formatted(suffix, categoryId, categoryId),
                201)
            .path("id")
            .asLong();

    mockMvc
        .perform(get("/api/v1/fee-plans/" + planId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.installments.length()").value(2));

    postJson(
            token,
            "/api/v1/batches",
            """
            {
              "courseId":%d,
              "academicYearId":%d,
              "code":"BAT_%s",
              "name":"Morning",
              "capacity":30,
              "startDate":"2026-05-01",
              "endDate":"2026-11-01"
            }
            """
                .formatted(courseId, yearId, suffix),
            201);

    String tokenB = login("admin", "Password@123", "DEMO_B");
    mockMvc
        .perform(get("/api/v1/courses/" + courseId).header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isNotFound());

    String platform = login("platform", "Password@123", null);
    mockMvc
        .perform(
            post("/api/v1/courses")
                .header("Authorization", "Bearer " + platform)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"X\",\"name\":\"Nope\"}"))
        .andExpect(status().isForbidden());
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
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
  }
}
