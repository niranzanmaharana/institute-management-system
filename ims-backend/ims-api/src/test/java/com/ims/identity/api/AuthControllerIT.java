package com.ims.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.JwtService;
import com.ims.platform.institute.infrastructure.InstituteJpaRepository;
import io.jsonwebtoken.Claims;
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
class AuthControllerIT {

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
  @Autowired private JwtService jwtService;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private InstituteJpaRepository instituteJpaRepository;

  @Test
  void shouldLoginInstituteAdmin() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"admin","password":"Password@123","instituteCode":"DEMO_A"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
        .andExpect(jsonPath("$.user.instituteId").isNumber());
  }

  @Test
  void shouldRejectBadPassword() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"admin","password":"wrong","instituteCode":"DEMO_A"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldLoginPlatformAdminWithoutInstitute() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"platform","password":"Password@123"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.roles[0]").value("PLATFORM_ADMIN"));
  }

  @Test
  void shouldIncludeInstituteIdClaimForInstituteUser() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"admin","password":"Password@123","instituteCode":"DEMO_A"}
                        """))
            .andExpect(status().isOk())
            .andReturn();

    String accessToken =
        objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    Claims claims = jwtService.parse(accessToken);
    assertThat(claims.getSubject()).isNotBlank();
    assertThat(claims.get("institute_id", Long.class)).isPositive();
  }

  @Test
  void shouldRefreshAndLogout() throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"admin","password":"Password@123","instituteCode":"DEMO_B"}
                        """))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
    String refreshToken = body.path("refreshToken").asText();
    String accessToken = body.path("accessToken").asText();

    MvcResult refreshed =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

    String newRefresh =
        objectMapper
            .readTree(refreshed.getResponse().getContentAsString())
            .path("refreshToken")
            .asText();

    mockMvc
        .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"));

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectInactiveUser() throws Exception {
    Long instituteId = instituteJpaRepository.findByCode("DEMO_A").orElseThrow().getId();
    UserAccount user =
        userAccountRepository
            .findByUsernameAndInstituteIdAndDeletedAtIsNull("admin", instituteId)
            .orElseThrow();
    user.deactivate();
    userAccountRepository.save(user);
    try {
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"username":"admin","password":"Password@123","instituteCode":"DEMO_A"}
                      """))
          .andExpect(status().isUnauthorized());
    } finally {
      UserAccount restored = userAccountRepository.findById(user.getId()).orElseThrow();
      restored.activate();
      userAccountRepository.save(restored);
    }
  }
}
