package com.ims.operations.api;

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
class TimetableIT {

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
  void roomAndFacultyConflictsRejectedAndBatchTimetableReturnsSlots() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());

    long yearId =
        postJson(
                token,
                "/api/v1/academic-years",
                "{\"code\":\"AY_TT_"
                    + suffix
                    + "\",\"name\":\"Year\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}",
                201)
            .path("id")
            .asLong();
    long courseId =
        postJson(
                token,
                "/api/v1/courses",
                "{\"code\":\"CRS_TT_" + suffix + "\",\"name\":\"TT Course\",\"description\":\"d\"}",
                201)
            .path("id")
            .asLong();
    long batchId =
        postJson(
                token,
                "/api/v1/batches",
                """
                {"courseId":%d,"academicYearId":%d,"code":"BAT_TT_%s","name":"Batch",
                 "capacity":30,"startDate":"2026-01-01","endDate":"2026-12-31"}
                """
                    .formatted(courseId, yearId, suffix),
                201)
            .path("id")
            .asLong();
    long facultyId =
        postJson(
                token,
                "/api/v1/faculties",
                """
                {"firstName":"Tea","lastName":"Cher","phone":"91%s","email":"fac_%s@ex.com"}
                """
                    .formatted(suffix.substring(Math.max(0, suffix.length() - 8)), suffix),
                201)
            .path("id")
            .asLong();

    long roomA =
        postJson(
                token,
                "/api/v1/rooms",
                "{\"name\":\"Room A " + suffix + "\",\"capacity\":40}",
                201)
            .path("id")
            .asLong();
    long roomB =
        postJson(
                token,
                "/api/v1/rooms",
                "{\"name\":\"Room B " + suffix + "\",\"capacity\":40}",
                201)
            .path("id")
            .asLong();

    postJson(
            token,
            "/api/v1/timetable/slots",
            """
            {"batchId":%d,"facultyId":%d,"roomId":%d,"dayOfWeek":1,
             "startTime":"09:00:00","endTime":"10:00:00"}
            """
                .formatted(batchId, facultyId, roomA),
            201);

    // room conflict
    mockMvc
        .perform(
            post("/api/v1/timetable/slots")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"batchId":%d,"roomId":%d,"dayOfWeek":1,
                     "startTime":"09:30:00","endTime":"10:30:00"}
                    """
                        .formatted(batchId, roomA)))
        .andExpect(status().isConflict());

    // faculty conflict in different room
    mockMvc
        .perform(
            post("/api/v1/timetable/slots")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"batchId":%d,"facultyId":%d,"roomId":%d,"dayOfWeek":1,
                     "startTime":"09:15:00","endTime":"09:45:00"}
                    """
                        .formatted(batchId, facultyId, roomB)))
        .andExpect(status().isConflict());

    // non-overlapping OK
    postJson(
            token,
            "/api/v1/timetable/slots",
            """
            {"batchId":%d,"facultyId":%d,"roomId":%d,"dayOfWeek":1,
             "startTime":"10:00:00","endTime":"11:00:00"}
            """
                .formatted(batchId, facultyId, roomA),
            201);

    mockMvc
        .perform(
            get("/api/v1/timetable/batches/" + batchId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.batchId").value(batchId))
        .andExpect(jsonPath("$.slots.length()").value(2));

    mockMvc
        .perform(
            get("/api/v1/timetable/batches/" + batchId + "?dayOfWeek=1")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots.length()").value(2));
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
