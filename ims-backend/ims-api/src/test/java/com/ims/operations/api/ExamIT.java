package com.ims.operations.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ExamIT {

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
  void examMarksPublishLockAndBlockEditWhileLocked() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());
    LocalDate examDate = LocalDate.now();

    long categoryId =
        postJson(
                token,
                "/api/v1/fee-categories",
                "{\"code\":\"TUIT_EX_" + suffix + "\",\"name\":\"Tuition\"}",
                201)
            .path("id")
            .asLong();
    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"AY_EX_"
                    + suffix
                    + "\",\"name\":\"Year\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}",
                201)
            .path("id")
            .asLong();
    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"CRS_EX_" + suffix + "\",\"name\":\"Exam Course\",\"description\":\"d\"}",
                201)
            .path("id")
            .asLong();
    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {"code":"FP_EX_%s","name":"Plan","currency":"INR","totalAmount":1000.00,
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
                {"courseId":%d,"academicYearId":%d,"code":"BAT_EX_%s","name":"Batch",
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
                {"firstName":"Eve","lastName":"Exam","phone":"88%s","email":"ex_%s@ex.com"}
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
                .header("Idempotency-Key", "act-ex-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"admissionWaiver\":true,\"waiverReason\":\"Exam IT setup path\"}"))
        .andExpect(status().isOk());

    long examId =
        postJson(
                token,
                "/api/v1/exams",
                """
                {"batchId":%d,"name":"Midterm %s","examDate":"%s"}
                """
                    .formatted(batchId, suffix, examDate),
                201)
            .path("id")
            .asLong();

    long theoryId =
        postJson(
                token,
                "/api/v1/exams/" + examId + "/components",
                """
                {"code":"THEORY","name":"Theory","maxMarks":100,"passMarks":40,"seq":1}
                """,
                201)
            .path("id")
            .asLong();

    mockMvc
        .perform(
            post("/api/v1/exams/" + examId + "/open-marks")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT_MARKS"));

    mockMvc
        .perform(
            get("/api/v1/exams/" + examId + "/results")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].studentId").value((int) studentId))
        .andExpect(jsonPath("$[0].resultStatus").value("MARKS"));

    // Partial / blank marks must save without 500 (draft incomplete)
    mockMvc
        .perform(
            put("/api/v1/exams/" + examId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"results":[{
                      "studentId":%d,
                      "resultStatus":"MARKS",
                      "componentMarks":[{"componentId":%d,"marks":null}]
                    }]}
                    """
                        .formatted(studentId, theoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].passFail").value(nullValue()))
        .andExpect(jsonPath("$[0].totalMarks").value(nullValue()));

    mockMvc
        .perform(
            put("/api/v1/exams/" + examId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"results":[{
                      "studentId":%d,
                      "resultStatus":"MARKS",
                      "componentMarks":[{"componentId":%d,"marks":55}]
                    }]}
                    """
                        .formatted(studentId, theoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].passFail").value("PASS"))
        .andExpect(jsonPath("$[0].totalMarks").value(55.0));

    mockMvc
        .perform(
            post("/api/v1/exams/" + examId + "/publish")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    mockMvc
        .perform(
            get("/api/v1/exams/" + examId + "/results")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    mockMvc
        .perform(
            post("/api/v1/exams/" + examId + "/lock")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LOCKED"));

    mockMvc
        .perform(
            put("/api/v1/exams/" + examId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"results":[{
                      "studentId":%d,
                      "resultStatus":"MARKS",
                      "componentMarks":[{"componentId":%d,"marks":60}]
                    }]}
                    """
                        .formatted(studentId, theoryId)))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/api/v1/exams/" + examId + "/unlock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Correction after lock\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT_MARKS"));
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
