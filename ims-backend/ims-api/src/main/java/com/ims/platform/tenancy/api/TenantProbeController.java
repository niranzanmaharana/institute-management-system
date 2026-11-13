package com.ims.platform.tenancy.api;

import com.ims.platform.tenancy.application.TenantProbeService;
import com.ims.platform.tenancy.domain.TenantProbe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/tenant-probes")
@Tag(name = "Tenant Probes", description = "Phase 0 isolation harness endpoints")
public class TenantProbeController {

  private final TenantProbeService tenantProbeService;

  public TenantProbeController(TenantProbeService tenantProbeService) {
    this.tenantProbeService = tenantProbeService;
  }

  public record CreateProbeRequest(@NotBlank String label) {}

  public record ProbeResponse(Long id, Long instituteId, String label) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create probe for current tenant")
  public ProbeResponse create(@Valid @RequestBody CreateProbeRequest request) {
    TenantProbe probe = tenantProbeService.create(request.label());
    return toResponse(probe);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get probe by id (tenant scoped)")
  public ProbeResponse get(@PathVariable("id") Long id) {
    return tenantProbeService
        .findByIdForCurrentTenant(id)
        .map(this::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping
  @Operation(summary = "List probes for current tenant")
  public List<ProbeResponse> list() {
    return tenantProbeService.listForCurrentTenant().stream().map(this::toResponse).toList();
  }

  private ProbeResponse toResponse(TenantProbe probe) {
    return new ProbeResponse(probe.getId(), probe.getInstituteId(), probe.getLabel());
  }
}
