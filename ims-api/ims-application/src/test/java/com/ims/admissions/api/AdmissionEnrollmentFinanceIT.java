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

    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/adjustments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"WAIVER\",\"amount\":1000.00,\"reason\":\"Hardship waiver\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.adjustment.type").value("WAIVER"))
        .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(7000.00));

    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/adjustments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"PENALTY\",\"amount\":250.00,\"reason\":\"Late fee charge\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(7250.00));

    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/adjustments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"type\":\"DISCOUNT\",\"amount\":99999.00,\"reason\":\"Too large discount\"}"))
        .andExpect(status().isBadRequest());

    postJson(
        token,
        "/api/v1/users",
        """
        {
          "username":"desk_%s",
          "email":"desk%s@example.com",
          "password":"Password@123",
          "roles":["FRONT_DESK"]
        }
        """
            .formatted(suffix, suffix),
        201);
    String deskToken = login("desk_" + suffix, "Password@123", "DEMO_A");
    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/adjustments")
                .header("Authorization", "Bearer " + deskToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"WAIVER\",\"amount\":10.00,\"reason\":\"Should be forbidden\"}"))
        .andExpect(status().isForbidden());

    String tokenB = login("admin", "Password@123", "DEMO_B");
    mockMvc
        .perform(
            get("/api/v1/fee-accounts/" + feeAccountId + "/adjustments")
                .header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/refunds")
                .header("Authorization", "Bearer " + deskToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10.00,\"reason\":\"Should be forbidden\"}"))
        .andExpect(status().isForbidden());

    long refundId =
        postJson(
                token,
                "/api/v1/fee-accounts/" + feeAccountId + "/refunds",
                "{\"amount\":500.00,\"reason\":\"Student withdrew part\"}",
                201)
            .path("refund")
            .path("id")
            .asLong();
    mockMvc
        .perform(
            post("/api/v1/refunds/" + refundId + "/approve")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    mockMvc
        .perform(
            post("/api/v1/refunds/" + refundId + "/process")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"success\":true,\"note\":\"Paid out\"}"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/refunds/" + refundId + "/process")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "rf-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"success\":true,\"note\":\"Paid out\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refund.status").value("COMPLETED"))
        .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(7750.00));
    mockMvc
        .perform(
            post("/api/v1/refunds/" + refundId + "/process")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "rf-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"success\":true,\"note\":\"Paid out\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refund.status").value("COMPLETED"));

    long failRefundId =
        postJson(
                token,
                "/api/v1/fee-accounts/" + feeAccountId + "/refunds",
                "{\"amount\":100.00,\"reason\":\"Bank payout attempt\"}",
                201)
            .path("refund")
            .path("id")
            .asLong();
    mockMvc
        .perform(
            post("/api/v1/refunds/" + failRefundId + "/approve")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/v1/refunds/" + failRefundId + "/process")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "rf-fail-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"success\":false,\"note\":\"Bank rejected\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refund.status").value("FAILED"))
        .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(7750.00));

    mockMvc
        .perform(
            post("/api/v1/fee-accounts/" + feeAccountId + "/refunds")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":99999.00,\"reason\":\"Exceeds collections\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/api/v1/refunds").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThan(0))));

    MvcResult paymentsResult =
        mockMvc
            .perform(
                get("/api/v1/fee-accounts/" + feeAccountId + "/payments")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThan(0))))
            .andReturn();
    long paymentId =
        objectMapper
            .readTree(paymentsResult.getResponse().getContentAsString())
            .get(0)
            .path("id")
            .asLong();

    mockMvc
        .perform(
            post("/api/v1/payments/" + paymentId + "/reverse")
                .header("Authorization", "Bearer " + deskToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Should be forbidden\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/payments/" + paymentId + "/reverse")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Wrong amount entered\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payment.status").value("REVERSED"))
        .andExpect(jsonPath("$.receipt.status").value("CANCELLED"))
        .andExpect(jsonPath("$.feeAccount.outstandingAmount").value(9750.00));

    mockMvc
        .perform(
            post("/api/v1/payments/" + paymentId + "/reverse")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Already reversed once\"}"))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            get("/api/v1/fee-accounts/" + feeAccountId + "/invoices")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("DUE"))
        .andExpect(jsonPath("$[0].amountPaid").value(0.00));

    mockMvc
        .perform(get("/api/v1/finance/reports/aging").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.buckets", hasSize(5)))
        .andExpect(jsonPath("$.invoices", hasSize(greaterThan(0))));

    mockMvc
        .perform(
            get("/api/v1/finance/reports/outstanding").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.feeAccountId == " + feeAccountId + ")]").exists());

    mockMvc
        .perform(
            get("/api/v1/finance/reports/daily-collection")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/finance/reports/collection-by-method")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/finance/reports/collection-by-category")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    long fyId =
        postJson(
                token,
                "/api/v1/financial-years",
                """
                {
                  "name":"FY-IT-%s",
                  "startDate":"2026-04-01",
                  "endDate":"2027-03-31",
                  "activate":true
                }
                """
                    .formatted(suffix),
                201)
            .path("id")
            .asLong();
    mockMvc
        .perform(get("/api/v1/financial-years").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + fyId + ")].active").value(true));

    mockMvc
        .perform(
            get("/api/v1/fee-accounts/" + feeAccountId + "/statement")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feeAccount.id").value(feeAccountId))
        .andExpect(jsonPath("$.totals.outstandingAmount").value(9750.00))
        .andExpect(jsonPath("$.invoices", hasSize(2)));

    MvcResult pdfResult =
        mockMvc
            .perform(
                get("/api/v1/fee-accounts/" + feeAccountId + "/statement.pdf")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(pdfResult.getResponse().getContentType()).contains("application/pdf");
    assertThat(pdfResult.getResponse().getContentAsByteArray().length).isGreaterThan(100);
    assertThat(pdfResult.getResponse().getContentAsByteArray()[0]).isEqualTo((byte) '%');
    assertThat(pdfResult.getResponse().getContentAsByteArray()[1]).isEqualTo((byte) 'P');

    mockMvc
        .perform(
            get("/api/v1/fee-accounts/" + feeAccountId + "/statement")
                .header("Authorization", "Bearer " + deskToken))
        .andExpect(status().isForbidden());

    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "STUDENT_CREATED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "ADMISSION_APPROVED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "ENROLLMENT_ACTIVATED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "PAYMENT_SUCCESS"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "FEE_ADJUSTED"))
        .isGreaterThanOrEqualTo(2);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "REFUND_COMPLETED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "REFUND_FAILED"))
        .isGreaterThanOrEqualTo(1);
    assertThat(auditLogJpaRepository.countByInstituteIdAndAction(1L, "PAYMENT_REVERSED"))
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
