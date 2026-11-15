package com.ims.imports.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
class CsvImportIT {

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
  void importStudents_insertsGoodRows_andReportsBadRows() throws Exception {
    String token = login("admin", "Password@123", "DEMO_A");
    String suffix = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(get("/api/v1/imports/students/template").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("firstName")));

    String csv =
        """
        studentCode,firstName,lastName,phone,email
        ,Good,One,9000000001,good1_%s@ex.com
        ,Bad,,9000000002,bad_%s@ex.com
        IMP%s,Good,Two,9000000003,good2_%s@ex.com
        """
            .formatted(suffix, suffix, suffix.substring(Math.max(0, suffix.length() - 6)), suffix);

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "students.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/api/v1/imports/students")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entityType").value("STUDENT"))
        .andExpect(jsonPath("$.totalRows").value(3))
        .andExpect(jsonPath("$.inserted").value(2))
        .andExpect(jsonPath("$.failed").value(1))
        .andExpect(jsonPath("$.insertedIds", hasSize(2)))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errorReportCsv", containsString("rowNumber")));

    String facultyCsv =
        """
        facultyCode,firstName,lastName,phone,email,department
        ,Teach,One,9100000001,fac1_%s@ex.com,Math
        ,,,9100000002,facbad_%s@ex.com,Science
        """
            .formatted(suffix, suffix);
    MockMultipartFile facultyFile =
        new MockMultipartFile(
            "file",
            "faculties.csv",
            "text/csv",
            facultyCsv.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/api/v1/imports/faculties")
                .file(facultyFile)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entityType").value("FACULTY"))
        .andExpect(jsonPath("$.inserted").value(1))
        .andExpect(jsonPath("$.failed").value(1));
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
}
