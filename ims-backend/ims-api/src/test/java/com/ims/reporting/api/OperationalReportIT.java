package com.ims.reporting.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
class OperationalReportIT {

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
  void starredReports_returnOk_forAdmin() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());
    LocalDate today = LocalDate.now();

    long categoryId =
        postJson(
                token,
                "/api/v1/fee-categories",
                "{\"code\":\"TUIT_RP_" + suffix + "\",\"name\":\"Tuition\"}",
                201)
            .path("id")
            .asLong();
    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"AY_RP_"
                    + suffix
                    + "\",\"name\":\"Year\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}",
                201)
            .path("id")
            .asLong();
    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"CRS_RP_" + suffix + "\",\"name\":\"Reports\",\"description\":\"d\"}",
                201)
            .path("id")
            .asLong();
    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {"code":"FP_RP_%s","name":"Plan","currency":"INR","totalAmount":1000.00,
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
                {"courseId":%d,"academicYearId":%d,"code":"BAT_RP_%s","name":"Batch",
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
                {"firstName":"Rep","lastName":"Ort","phone":"66%s","email":"rp_%s@ex.com"}
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
                .header("Idempotency-Key", "act-rp-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"admissionWaiver\":true,\"waiverReason\":\"Reports IT setup path\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/reports/students")
                .param("batchId", String.valueOf(batchId))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].studentId").value((int) studentId));

    mockMvc
        .perform(
            get("/api/v1/reports/students/active-counts")
                .param("courseId", String.valueOf(courseId))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].activeCount", greaterThanOrEqualTo(1)));

    mockMvc
        .perform(
            get("/api/v1/reports/students/roster")
                .param("batchId", String.valueOf(batchId))
                .param("status", "ACTIVE")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].studentId").value((int) studentId))
        .andExpect(jsonPath("$[0].enrollmentStatus").value("ACTIVE"));

    mockMvc
        .perform(
            post("/api/v1/enrollments/" + enrollmentId + "/withdraw")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Reports IT withdrawal path\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/reports/students/withdrawals")
                .param("from", today.minusDays(1).toString())
                .param("to", today.toString())
                .param("batchId", String.valueOf(batchId))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].studentId").value((int) studentId))
        .andExpect(jsonPath("$[0].enrollmentStatus").value("WITHDRAWN"));

    mockMvc
        .perform(
            get("/api/v1/reports/admissions")
                .param("from", today.minusDays(1).toString())
                .param("to", today.toString())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/reports/adjustments")
                .param("from", today.minusDays(30).toString())
                .param("to", today.toString())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/reports/attendance/low")
                .param("batchId", String.valueOf(batchId))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.batchId").value((int) batchId));

    long examId =
        postJson(
                token,
                "/api/v1/exams",
                """
                {"batchId":%d,"name":"RP Exam %s","examDate":"%s"}
                """
                    .formatted(batchId, suffix, today),
                201)
            .path("id")
            .asLong();
    postJson(
        token,
        "/api/v1/exams/" + examId + "/components",
        "{\"code\":\"TH\",\"name\":\"Theory\",\"maxMarks\":100,\"passMarks\":40,\"seq\":1}",
        201);
    mockMvc
        .perform(
            post("/api/v1/exams/" + examId + "/open-marks")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/reports/exams/results")
                .param("batchId", String.valueOf(batchId))
                .param("examId", String.valueOf(examId))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].studentId").value((int) studentId));
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
