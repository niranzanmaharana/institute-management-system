package com.ims.operations.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.platform.audit.infrastructure.AuditLogJpaRepository;
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
class AttendanceIT {

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
  @Autowired private AuditLogJpaRepository auditLogJpaRepository;

  @Test
  void attendanceLifecycleMarkSubmitLockUnlockAndPercentage() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());
    LocalDate sessionDate = LocalDate.now();

    long categoryId =
        postJson(
                token,
                "/api/v1/fee-categories",
                "{\"code\":\"TUIT_AT_" + suffix + "\",\"name\":\"Tuition\"}",
                201)
            .path("id")
            .asLong();
    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"AY_AT_"
                    + suffix
                    + "\",\"name\":\"Year\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}",
                201)
            .path("id")
            .asLong();
    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"CRS_AT_" + suffix + "\",\"name\":\"Attendance Course\",\"description\":\"d\"}",
                201)
            .path("id")
            .asLong();
    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {"code":"FP_AT_%s","name":"Plan","currency":"INR","totalAmount":1000.00,
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
                {"courseId":%d,"academicYearId":%d,"code":"BAT_AT_%s","name":"Batch",
                 "capacity":30,"startDate":"2026-01-01","endDate":"2026-12-31"}
                """
                    .formatted(courseId, yearId, suffix),
                201)
            .path("id")
            .asLong();

    long student1 =
        postJson(
                token,
                "/api/v1/students",
                """
                {"firstName":"Ann","lastName":"One","phone":"9000000001","email":"a1_%s@ex.com"}
                """
                    .formatted(suffix),
                201)
            .path("id")
            .asLong();
    long student2 =
        postJson(
                token,
                "/api/v1/students",
                """
                {"firstName":"Bob","lastName":"Two","phone":"9000000002","email":"a2_%s@ex.com"}
                """
                    .formatted(suffix),
                201)
            .path("id")
            .asLong();

    long enr1 =
        postJson(
                token,
                "/api/v1/enrollments",
                "{\"studentId\":%d,\"batchId\":%d,\"feePlanId\":%d}"
                    .formatted(student1, batchId, planId),
                201)
            .path("id")
            .asLong();
    long enr2 =
        postJson(
                token,
                "/api/v1/enrollments",
                "{\"studentId\":%d,\"batchId\":%d,\"feePlanId\":%d}"
                    .formatted(student2, batchId, planId),
                201)
            .path("id")
            .asLong();
    String waiver =
        "{\"admissionWaiver\":true,\"waiverReason\":\"Attendance IT setup path\"}";
    mockMvc
        .perform(
            post("/api/v1/enrollments/" + enr1 + "/activate")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "act-at-1-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(waiver))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/v1/enrollments/" + enr2 + "/activate")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "act-at-2-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(waiver))
        .andExpect(status().isOk());

    long sessionId =
        postJson(
                token,
                "/api/v1/attendance/sessions",
                "{\"batchId\":%d,\"sessionDate\":\"%s\"}"
                    .formatted(batchId, sessionDate),
                200)
            .path("id")
            .asLong();

    mockMvc
        .perform(
            get("/api/v1/attendance/sessions/" + sessionId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.records", hasSize(2)));

    mockMvc
        .perform(
            put("/api/v1/attendance/sessions/" + sessionId + "/records")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"records":[
                      {"studentId":%d,"status":"PRESENT"},
                      {"studentId":%d,"status":"ABSENT"}
                    ]}
                    """
                        .formatted(student1, student2)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/attendance/sessions/" + sessionId + "/submit")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));

    mockMvc
        .perform(
            post("/api/v1/attendance/sessions/" + sessionId + "/lock")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LOCKED"));

    mockMvc
        .perform(
            put("/api/v1/attendance/sessions/" + sessionId + "/records")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"records\":[{\"studentId\":%d,\"status\":\"LATE\"}]}".formatted(student2)))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/api/v1/attendance/sessions/" + sessionId + "/unlock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Correction after lock\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));

    mockMvc
        .perform(
            put("/api/v1/attendance/sessions/" + sessionId + "/records")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"records":[
                      {"studentId":%d,"status":"PRESENT"},
                      {"studentId":%d,"status":"LATE"}
                    ]}
                    """
                        .formatted(student1, student2)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/attendance/students/" + student1 + "/summary?batchId=" + batchId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.presentEffective").value(1))
        .andExpect(jsonPath("$.absent").value(0))
        .andExpect(jsonPath("$.percentage").value(100.00));

    mockMvc
        .perform(
            get("/api/v1/attendance/students/" + student2 + "/summary?batchId=" + batchId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.presentEffective").value(1))
        .andExpect(jsonPath("$.percentage").value(100.00));

    mockMvc
        .perform(
            get("/api/v1/attendance/batches/" + batchId + "/summary")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionCount").value(1));

    assertThat(
            auditLogJpaRepository.countByInstituteIdAndAction(1L, "ATTENDANCE_UNLOCKED"))
        .isGreaterThanOrEqualTo(1);
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
