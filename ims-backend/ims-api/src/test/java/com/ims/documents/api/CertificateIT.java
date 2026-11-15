package com.ims.documents.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
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
class CertificateIT {

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
  void generateBonafideListAndDownload_andRejectIncompleteCompletion() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());

    long categoryId =
        postJson(
                token,
                "/api/v1/fee-categories",
                "{\"code\":\"TUIT_CERT_" + suffix + "\",\"name\":\"Tuition\"}",
                201)
            .path("id")
            .asLong();
    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"AY_CERT_"
                    + suffix
                    + "\",\"name\":\"Year\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}",
                201)
            .path("id")
            .asLong();
    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"CRS_CERT_"
                    + suffix
                    + "\",\"name\":\"Cert Course\",\"description\":\"d\"}",
                201)
            .path("id")
            .asLong();
    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {"code":"FP_CERT_%s","name":"Plan","currency":"INR","totalAmount":1000.00,
                 "installments":[{"seq":1,"feeCategoryId":%d,"label":"Fee","amount":1000.00,"dueOffsetDays":0}]}
                """
                    .formatted(suffix, categoryId),
                201)
            .path("id")
            .asLong();
    long batchId =
        postJson(
                token,
                "/api/v1/batches",
                """
                {"courseId":%d,"academicYearId":%d,"code":"BAT_CERT_%s","name":"Batch",
                 "capacity":30,"startDate":"2026-01-01","endDate":"2026-12-31"}
                """
                    .formatted(courseId, yearId, suffix),
                201)
            .path("id")
            .asLong();
    long studentId =
        postJson(
                token,
                "/api/v1/students",
                """
                {"firstName":"Cert","lastName":"Student","phone":"77%s","email":"cert_%s@ex.com"}
                """
                    .formatted(suffix.substring(Math.max(0, suffix.length() - 8)), suffix),
                201)
            .path("id")
            .asLong();
    long enrollmentId =
        postJson(
                token,
                "/api/v1/enrollments",
                "{\"studentId\":%d,\"batchId\":%d,\"feePlanId\":%d}"
                    .formatted(studentId, batchId, planId),
                201)
            .path("id")
            .asLong();
    mockMvc
        .perform(
            post("/api/v1/enrollments/" + enrollmentId + "/activate")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "act-cert-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"admissionWaiver\":true,\"waiverReason\":\"Certificate IT setup path\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/certificate-templates").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThan(0))));

    // COMPLETION while still ACTIVE → 409
    mockMvc
        .perform(
            post("/api/v1/certificates/generate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"studentId":%d,"templateCode":"COMPLETION","enrollmentId":%d}
                    """
                        .formatted(studentId, enrollmentId)))
        .andExpect(status().isConflict());

    long certId =
        postJson(
                token,
                "/api/v1/certificates/generate",
                """
                {"studentId":%d,"templateCode":"BONAFIDE","enrollmentId":%d}
                """
                    .formatted(studentId, enrollmentId),
                201)
            .path("id")
            .asLong();

    mockMvc
        .perform(
            get("/api/v1/students/" + studentId + "/certificates")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].templateCode").value("BONAFIDE"));

    mockMvc
        .perform(
            get("/api/v1/certificates/" + certId + "/download")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url", not(emptyOrNullString())))
        .andExpect(jsonPath("$.expiresInSeconds").value(greaterThan(0)));

    mockMvc
        .perform(
            post("/api/v1/enrollments/" + enrollmentId + "/complete")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Finished course successfully\"}"))
        .andExpect(status().isOk());

    postJson(
        token,
        "/api/v1/certificates/generate",
        """
        {"studentId":%d,"templateCode":"COMPLETION","enrollmentId":%d}
        """
            .formatted(studentId, enrollmentId),
        201);
  }

  private String login(String username, String password, String instituteCode) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"%s","password":"%s","instituteCode":"%s"}
                        """
                            .formatted(username, password, instituteCode)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .path("accessToken")
        .asText();
  }

  private JsonNode postJson(String token, String path, String body, int expectedStatus)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(path)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(expectedStatus))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
}
