package com.ims.admissions.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.platform.audit.infrastructure.AuditLogJpaRepository;
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
class AdmissionEnrollmentFinanceIT {

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
  void admissionApproveEnrollActivatePartialPayAndOutstanding() throws Exception {
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
                "{\"code\":\"CRS_" + suffix + "\",\"name\":\"Java\",\"description\":\"Demo\"}",
                201)
            .path("id")
            .asLong();
    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {
                  "code":"FP_%s",
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
    long batchId =
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
                201)
            .path("id")
            .asLong();

    long admissionId =
        postJson(
                token,
                "/api/v1/admissions",
                """
                {
                  "courseId":%d,
                  "applicantName":"Ada Lovelace",
                  "phone":"90000%s",
                  "email":"ada%s@example.com"
                }
                """
                    .formatted(courseId, suffix.substring(suffix.length() - 4), suffix),
                201)
            .path("id")
            .asLong();

    JsonNode approved =
        postJson(
            token,
            "/api/v1/admissions/" + admissionId + "/approve",
            """
            {
              "createStudent":{
                "studentCode":"STU_%s",
                "firstName":"Ada",
                "lastName":"Lovelace",
                "phone":"90000%s",
                "email":"ada%s@example.com"
              },
              "reason":"Eligible"
            }
            """
                .formatted(suffix, suffix.substring(suffix.length() - 4), suffix),
            200);
    long studentId = approved.path("studentId").asLong();

    mockMvc
        .perform(get("/api/v1/admissions").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));

    long enrollmentId =
        postJson(
                token,
                "/api/v1/enrollments",
                """
                {"studentId":%d,"batchId":%d,"feePlanId":%d}
                """
                    .formatted(studentId, batchId, planId),
                201)
            .path("id")
            .asLong();

    MvcResult activated =
        mockMvc
            .perform(
                post("/api/v1/enrollments/" + enrollmentId + "/activate")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", "act-" + suffix))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enrollmentStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(10000.00))
            .andExpect(jsonPath("$.invoices", hasSize(2)))
            .andReturn();
    long feeAccountId =
        objectMapper
            .readTree(activated.getResponse().getContentAsString())
            .path("feeAccount")
            .path("id")
            .asLong();

    mockMvc
        .perform(
            post("/api/v1/enrollments/" + enrollmentId + "/activate")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "act-" + suffix))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feeAccount.id").value(feeAccountId));

    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/payments")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "pay-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":2000.00,\"method\":\"CASH\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payment.amount").value(2000.00))
        .andExpect(jsonPath("$.receipt.receiptNo").value(org.hamcrest.Matchers.startsWith("RCP-")))
        .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(8000.00));

    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/payments")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "pay-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":2000.00,\"method\":\"CASH\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(8000.00));

    mockMvc
        .perform(get("/api/v1/fee-accounts/" + feeAccountId + "/invoices")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PAID"))
        .andExpect(jsonPath("$[1].status").value("DUE"));

    mockMvc
        .perform(get("/api/v1/outstanding").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + feeAccountId + ")].outstandingAmount").exists());

    mockMvc
        .perform(
            get("/api/v1/fee-accounts?studentId=" + studentId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(feeAccountId));

    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "STUDENT_CREATED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "ADMISSION_APPROVED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "ENROLLMENT_ACTIVATED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "PAYMENT_SUCCESS"))
        .isGreaterThanOrEqualTo(1);
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
