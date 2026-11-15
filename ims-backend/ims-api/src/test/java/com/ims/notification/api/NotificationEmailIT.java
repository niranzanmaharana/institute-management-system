package com.ims.notification.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.notification.application.NotificationService;
import com.ims.notification.application.NotificationTrigger;
import com.ims.notification.application.channel.LoggingEmailChannelSender;
import com.ims.notification.domain.NotificationDelivery;
import com.ims.notification.domain.NotificationDeliveryStatus;
import com.ims.notification.infrastructure.NotificationDeliveryJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
class NotificationEmailIT {

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
    registry.add("ims.notification.channel", () -> "logging");
    registry.add("ims.notification.enabled", () -> "true");
    registry.add("ims.notification.initial-retry-seconds", () -> "1");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private LoggingEmailChannelSender emailSink;
  @Autowired private NotificationDeliveryJpaRepository deliveryRepository;
  @Autowired private NotificationService notificationService;

  @BeforeEach
  void clearSink() {
    emailSink.clear();
  }

  @Test
  void admissionApprovedAndPaymentReceivedDeliverToEmailSink() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());

    long categoryId =
        postJson(
                token,
                "/api/v1/fee-categories",
                "{\"code\":\"NT_TUIT_" + suffix + "\",\"name\":\"Tuition\"}",
                201)
            .path("id")
            .asLong();
    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"NT_AY_"
                    + suffix
                    + "\",\"name\":\"Year\",\"startDate\":\"2026-04-01\",\"endDate\":\"2027-03-31\"}",
                201)
            .path("id")
            .asLong();
    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"NT_CRS_" + suffix + "\",\"name\":\"Notify Course\",\"description\":\"x\"}",
                201)
            .path("id")
            .asLong();
    long planId =
        postJson(
                token,
                "/api/v1/courses/" + courseId + "/fee-plans",
                """
                {
                  "code":"NT_FP_%s",
                  "name":"Default",
                  "currency":"INR",
                  "totalAmount":5000.00,
                  "installments":[
                    {"seq":1,"feeCategoryId":%d,"label":"Fee","amount":5000.00,"dueOffsetDays":0}
                  ]
                }
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
                {
                  "courseId":%d,
                  "academicYearId":%d,
                  "code":"NT_BAT_%s",
                  "name":"Batch",
                  "capacity":20,
                  "startDate":"2026-05-01",
                  "endDate":"2026-11-01"
                }
                """
                    .formatted(courseId, yearId, suffix),
                201)
            .path("id")
            .asLong();

    String email = "notify_" + suffix + "@example.com";
    long admissionId =
        postJson(
                token,
                "/api/v1/admissions",
                """
                {
                  "courseId":%d,
                  "applicantName":"Notify Applicant",
                  "phone":"91%s",
                  "email":"%s"
                }
                """
                    .formatted(courseId, suffix.substring(suffix.length() - 8), email),
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
                "studentCode":"NT_%s",
                "firstName":"Notify",
                "lastName":"Student",
                "phone":"91%s",
                "email":"%s"
              },
              "reason":"Eligible for notify test"
            }
            """
                .formatted(suffix, suffix.substring(suffix.length() - 8), email),
            200);
    long studentId = approved.path("studentId").asLong();

    awaitDelivery(admissionId, "AdmissionApplication", NotificationTrigger.ADMISSION_APPROVED);

    long enrollmentId =
        postJson(
                token,
                "/api/v1/enrollments",
                "{\"studentId\":%d,\"batchId\":%d,\"feePlanId\":%d}"
                    .formatted(studentId, batchId, planId),
                201)
            .path("id")
            .asLong();

    MvcResult activated =
        mockMvc
            .perform(
                post("/api/v1/enrollments/" + enrollmentId + "/activate")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", "nt-act-" + suffix))
            .andExpect(status().isOk())
            .andReturn();
    long feeAccountId =
        objectMapper
            .readTree(activated.getResponse().getContentAsString())
            .path("feeAccount")
            .path("id")
            .asLong();

    JsonNode payment =
        postJson(
            token,
            "/api/v1/fee-accounts/" + feeAccountId + "/payments",
            "{\"amount\":1000.00,\"method\":\"UPI\"}",
            200,
            "nt-pay-" + suffix);
    long paymentId = payment.path("payment").path("id").asLong();

    awaitDelivery(paymentId, "Payment", NotificationTrigger.PAYMENT_RECEIVED);

    List<LoggingEmailChannelSender.SentMessage> sent = emailSink.snapshot();
    assertThat(sent).hasSizeGreaterThanOrEqualTo(2);
    assertThat(sent)
        .anySatisfy(
            m -> {
              assertThat(m.to()).isEqualTo(email);
              assertThat(m.subject()).contains("Admission approved");
            });
    assertThat(sent)
        .anySatisfy(
            m -> {
              assertThat(m.to()).isEqualTo(email);
              assertThat(m.subject()).contains("Payment received");
              assertThat(m.body()).contains("1000");
            });
  }

  @Test
  void failedSendIsRecordedAndRetried() {
    emailSink.clear();
    // Force a send failure by using an invalid "smtp-like" path: enqueue with a custom failing
    // attempt via repository + processDueRetries after marking next retry.
    NotificationDelivery pending =
        NotificationDelivery.create(
            1L,
            NotificationTrigger.ADMISSION_APPROVED,
            "EMAIL",
            NotificationTrigger.ADMISSION_APPROVED,
            "retry@example.com",
            "Retry subject",
            "Retry body",
            "AdmissionApplication",
            999001L,
            3);
    // Simulate a prior failed attempt awaiting retry.
    pending.markAttemptFailed("simulated smtp down", Instant.now().minusSeconds(1), false);
    pending = deliveryRepository.save(pending);
    assertThat(pending.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
    assertThat(pending.getAttemptCount()).isEqualTo(1);
    assertThat(pending.getLastError()).contains("simulated smtp down");

    int processed = notificationService.processDueRetries();
    assertThat(processed).isGreaterThanOrEqualTo(1);

    NotificationDelivery after = deliveryRepository.findById(pending.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
    assertThat(after.getAttemptCount()).isEqualTo(2);
    assertThat(emailSink.snapshot())
        .anySatisfy(m -> assertThat(m.to()).isEqualTo("retry@example.com"));
  }

  @Test
  void missingRecipientIsSkippedAndLogged() {
    var result =
        notificationService.enqueue(
            1L,
            NotificationTrigger.ADMISSION_APPROVED,
            NotificationTrigger.ADMISSION_APPROVED,
            "  ",
            Map.of("applicantName", "X", "applicationNo", "A1", "instituteName", "Demo"),
            "AdmissionApplication",
            999002L);
    assertThat(result).isPresent();
    assertThat(result.get().getStatus()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
  }

  private void awaitDelivery(long entityId, String entityType, String trigger) throws Exception {
    Instant deadline = Instant.now().plusSeconds(10);
    while (Instant.now().isBefore(deadline)) {
      List<NotificationDelivery> rows =
          deliveryRepository.findAll().stream()
              .filter(d -> entityType.equals(d.getRelatedEntityType()))
              .filter(d -> Long.valueOf(entityId).equals(d.getRelatedEntityId()))
              .filter(d -> trigger.equals(d.getTriggerCode()))
              .toList();
      if (!rows.isEmpty()
          && rows.stream().anyMatch(d -> NotificationDeliveryStatus.SENT.equals(d.getStatus()))) {
        return;
      }
      Thread.sleep(150);
    }
    throw new AssertionError(
        "Timed out waiting for SENT delivery trigger=" + trigger + " entity=" + entityId);
  }

  private String login(String username, String password, String instituteCode) throws Exception {
    String body =
        """
        {"username":"%s","password":"%s","instituteCode":"%s"}
        """
            .formatted(username, password, instituteCode);
    MvcResult result =
        mockMvc
            .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
  }

  private JsonNode postJson(String token, String path, String json, int expectedStatus)
      throws Exception {
    return postJson(token, path, json, expectedStatus, null);
  }

  private JsonNode postJson(
      String token, String path, String json, int expectedStatus, String idempotencyKey)
      throws Exception {
    var req =
        post(path)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json);
    if (idempotencyKey != null) {
      req.header("Idempotency-Key", idempotencyKey);
    }
    MvcResult result = mockMvc.perform(req).andExpect(status().is(expectedStatus)).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
}
