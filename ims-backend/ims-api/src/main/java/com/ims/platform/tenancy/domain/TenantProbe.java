package com.ims.platform.tenancy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "tenant_probes")
@FilterDef(name = "instituteFilter", parameters = @ParamDef(name = "instituteId", type = Long.class))
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class TenantProbe {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 128)
  private String label;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected TenantProbe() {}

  public TenantProbe(Long instituteId, String label) {
    this.instituteId = instituteId;
    this.label = label;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getLabel() {
    return label;
  }
}
