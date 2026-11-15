package com.ims.imports.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class MarksBulkImportIT {

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
  void bulkMarksUpload_idempotentUpsert_andJobStatus() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());
    LocalDate examDate = LocalDate.now();

    long categoryId =
        postJson(
                token,
                "/api/v1/fee-categories",
                "{\"code\":\"TUIT_MI_" + suffix + "\",\"name\":\"Tuition\"}",
                201)
            .path("id")
            .asLong();
    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"AY_MI_"
                    + suffix
                    + "\",\"name\":\"Year\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}",
                201)
            .path("id")
            .asLong();
    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"CRS_MI_" + suffix + "\",\"name\":\"Marks Import\",\"description\":\"d\"}",
                201)
            .path("id")
            .asLong();
    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {"code":"FP_MI_%s","name":"Plan","currency":"INR","totalAmount":1000.00,
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
                {"courseId":%d,"academicYearId":%d,"code":"BAT_MI_%s","name":"Batch",
                 "capacity":30,"startDate":"2026-01-01","endDate":"2026-12-31"}
                """
                    .formatted(courseId, yearId, suffix),
                201)
            .path("id")
            .asLong();
    JsonNode student =
        postJson(
            token,
            "/api/v1/students",
            """
            {"firstName":"Mia","lastName":"Marks","phone":"77%s","email":"mi_%s@ex.com"}
            """
                .formatted(suffix.substring(Math.max(0, suffix.length() - 8)), suffix),
            201);
    long studentId = student.path("id").asLong();
    String studentCode = student.path("studentCode").asText();
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
                .header("Idempotency-Key", "act-mi-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"admissionWaiver\":true,\"waiverReason\":\"Marks import IT setup\"}"))
        .andExpect(status().isOk());

    long examId =
        postJson(
                token,
                "/api/v1/exams",
                """
                {"batchId":%d,"name":"Import Mid %s","examDate":"%s"}
                """
                    .formatted(batchId, suffix, examDate),
                201)
            .path("id")
            .asLong();
    postJson(
        token,
        "/api/v1/exams/" + examId + "/components",
        """
        {"code":"THEORY","name":"Theory","maxMarks":100,"passMarks":40,"seq":1}
        """,
        201);
    mockMvc
        .perform(
            post("/api/v1/exams/" + examId + "/open-marks")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT_MARKS"));

    mockMvc
        .perform(
            get("/api/v1/exams/" + examId + "/marks-import/template")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("studentCode")))
        .andExpect(content().string(containsString("THEORY")));

    String csv =
        """
        studentCode,resultStatus,graceMarks,grade,remarks,THEORY
        %s,MARKS,,B,ok,72
        """
            .formatted(studentCode);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "marks.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

    MvcResult upload =
        mockMvc
            .perform(
                multipart("/api/v1/exams/" + examId + "/marks-import")
                    .file(file)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.failedCount").value(0))
            .andExpect(jsonPath("$.async").value(false))
            .andReturn();
    long jobId =
        objectMapper
            .readTree(upload.getResponse().getContentAsString())
            .path("id")
            .asLong();

    mockMvc
        .perform(
            get("/api/v1/marks-import-jobs/" + jobId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.examId").value((int) examId));

    mockMvc
        .perform(get("/api/v1/exams/" + examId + "/results").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].totalMarks").value(72.0))
        .andExpect(jsonPath("$[0].grade").value("B"));

    // Re-upload same file — idempotent upsert (still 1 result, updated marks)
    String csv2 =
        """
        studentCode,resultStatus,graceMarks,grade,remarks,THEORY
        %s,MARKS,,A,reimport,80
        """
            .formatted(studentCode);
    MockMultipartFile file2 =
        new MockMultipartFile(
            "file", "marks2.csv", "text/csv", csv2.getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            multipart("/api/v1/exams/" + examId + "/marks-import")
                .file(file2)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.successCount").value(1));

    mockMvc
        .perform(get("/api/v1/exams/" + examId + "/results").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].totalMarks").value(80.0))
        .andExpect(jsonPath("$[0].grade").value("A"));
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
