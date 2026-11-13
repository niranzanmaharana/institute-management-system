package com.ims.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.ims.common.tenancy.TenantContext;
import com.ims.platform.institute.infrastructure.InstituteJpaRepository;
import com.ims.platform.tenancy.application.TenantProbeService;
import com.ims.platform.tenancy.domain.TenantProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TenantIsolationIT {

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("ims")
          .withUsername("ims")
          .withPassword("ims");

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired private TenantProbeService tenantProbeService;
  @Autowired private InstituteJpaRepository instituteJpaRepository;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void shouldSeedTwoDemoInstitutes() {
    assertThat(instituteJpaRepository.count()).isEqualTo(2);
    assertThat(instituteJpaRepository.findByCode("DEMO_A")).isPresent();
    assertThat(instituteJpaRepository.findByCode("DEMO_B")).isPresent();
  }

  @Test
  void shouldNotReturnOtherTenantProbeById() {
    long instituteA = instituteJpaRepository.findByCode("DEMO_A").orElseThrow().getId();
    long instituteB = instituteJpaRepository.findByCode("DEMO_B").orElseThrow().getId();

    TenantContext.setInstituteId(instituteA);
    TenantProbe created = tenantProbeService.create("probe-a");

    TenantContext.setInstituteId(instituteB);
    assertThat(tenantProbeService.findByIdForCurrentTenant(created.getId())).isEmpty();

    TenantContext.setInstituteId(instituteA);
    assertThat(tenantProbeService.findByIdForCurrentTenant(created.getId())).isPresent();
  }
}
