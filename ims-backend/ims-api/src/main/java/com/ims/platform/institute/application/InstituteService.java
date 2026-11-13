package com.ims.platform.institute.application;

import com.ims.identity.config.IdentityDataLoader;
import com.ims.identity.domain.Role;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.RoleRepository;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.platform.codes.application.CodeGeneratorService;
import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.institute.api.InstituteController.CreateInstituteRequest;
import com.ims.platform.institute.api.InstituteController.InitialAdminRequest;
import com.ims.platform.institute.api.InstituteController.InstituteProfileRequest;
import com.ims.platform.institute.api.InstituteController.UpdateInstituteRequest;
import com.ims.platform.institute.api.InstituteResponse;
import com.ims.platform.institute.domain.Institute;
import com.ims.platform.institute.infrastructure.InstituteJpaRepository;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InstituteService {

  private static final Logger log = LoggerFactory.getLogger(InstituteService.class);

  private final InstituteJpaRepository instituteJpaRepository;
  private final UserAccountRepository userAccountRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final CodeGeneratorService codeGeneratorService;

  public InstituteService(
      InstituteJpaRepository instituteJpaRepository,
      UserAccountRepository userAccountRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      CodeGeneratorService codeGeneratorService) {
    this.instituteJpaRepository = instituteJpaRepository;
    this.userAccountRepository = userAccountRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.codeGeneratorService = codeGeneratorService;
  }

  @Transactional(readOnly = true)
  public List<InstituteResponse> listVisible(Long scopedInstituteId) {
    if (scopedInstituteId == null) {
      return instituteJpaRepository.findAll().stream().map(this::toResponse).toList();
    }
    return instituteJpaRepository
        .findById(scopedInstituteId)
        .map(institute -> List.of(toResponse(institute)))
        .orElse(List.of());
  }

  @Transactional(readOnly = true)
  public InstituteResponse getVisible(Long id, ImsUserPrincipal principal) {
    Institute institute =
        instituteJpaRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found"));
    assertCanView(principal, institute.getId());
    return toResponse(institute);
  }

  @Transactional
  public InstituteResponse create(CreateInstituteRequest request) {
    String normalized = codeGeneratorService.resolve(CodeEntityType.INSTITUTE, request.code());
    if (instituteJpaRepository.findByCode(normalized).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Institute code already exists");
    }
    String timezone =
        request.timezone() == null || request.timezone().isBlank()
            ? "Asia/Kolkata"
            : request.timezone().trim();
    Institute institute = Institute.create(normalized, request.name().trim(), timezone);
    applyProfile(institute, request.name(), timezone, request.profile());
    Institute saved = instituteJpaRepository.save(institute);
    if (request.initialAdmin() != null) {
      bootstrapAdmin(saved.getId(), request.initialAdmin());
    }
    log.info(
        "Institute created id={} code={} initialAdmin={}",
        saved.getId(),
        saved.getCode(),
        request.initialAdmin() != null);
    return toResponse(saved);
  }

  @Transactional
  public InstituteResponse update(
      Long id, UpdateInstituteRequest request, ImsUserPrincipal principal) {
    Institute institute =
        instituteJpaRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found"));
    assertCanUpdate(principal, institute.getId());
    applyProfile(institute, request.name(), request.timezone(), request.profile());
    Institute saved = instituteJpaRepository.save(institute);
    log.info("Institute updated id={} code={}", saved.getId(), saved.getCode());
    return toResponse(saved);
  }

  @Transactional
  public InstituteResponse suspend(Long id) {
    Institute institute =
        instituteJpaRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found"));
    institute.suspend();
    Institute saved = instituteJpaRepository.save(institute);
    log.info("Institute suspended id={} code={}", saved.getId(), saved.getCode());
    return toResponse(saved);
  }

  private void assertCanView(ImsUserPrincipal principal, Long instituteId) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    boolean platform =
        principal.getAuthorities().stream()
            .anyMatch(a -> "ROLE_PLATFORM_ADMIN".equals(a.getAuthority()));
    if (platform) {
      return;
    }
    if (principal.getInstituteId() == null || !principal.getInstituteId().equals(instituteId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found");
    }
  }

  private void assertCanUpdate(ImsUserPrincipal principal, Long instituteId) {
    assertCanView(principal, instituteId);
    boolean platform =
        principal.getAuthorities().stream()
            .anyMatch(a -> "ROLE_PLATFORM_ADMIN".equals(a.getAuthority()));
    if (platform) {
      return;
    }
    // ADMIN may update own institute profile
  }

  private void applyProfile(
      Institute institute, String name, String timezone, InstituteProfileRequest profile) {
    if (profile == null) {
      institute.applyProfile(
          name, timezone, null, null, null, null, null, null, null, null, null, null);
      return;
    }
    String adminEmail = blankToNull(profile.adminEmail());
    if (adminEmail != null && !adminEmail.contains("@")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid admin email");
    }
    institute.applyProfile(
        name,
        timezone,
        profile.mobile(),
        adminEmail,
        profile.website(),
        profile.addressLine1(),
        profile.addressLine2(),
        profile.city(),
        profile.state(),
        profile.postalCode(),
        profile.country(),
        profile.iconUrl());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private void bootstrapAdmin(Long instituteId, InitialAdminRequest initialAdmin) {
    String username = initialAdmin.username().trim();
    String email = initialAdmin.email().trim();
    String rawPassword =
        initialAdmin.password() == null || initialAdmin.password().isBlank()
            ? IdentityDataLoader.DEFAULT_PASSWORD
            : initialAdmin.password();

    if (userAccountRepository.findByUsernameAndInstituteId(username, instituteId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin username already exists");
    }
    if (userAccountRepository.findByEmailAndInstituteId(email, instituteId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin email already exists");
    }

    Role adminRole =
        roleRepository
            .findByCode("ADMIN")
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN role not seeded"));

    userAccountRepository.save(
        new UserAccount(
            instituteId, username, email, passwordEncoder.encode(rawPassword), Set.of(adminRole)));
  }

  private InstituteResponse toResponse(Institute institute) {
    return new InstituteResponse(
        institute.getId(),
        institute.getCode(),
        institute.getName(),
        institute.getStatus(),
        institute.getTimezone(),
        institute.getMobile(),
        institute.getAdminEmail(),
        institute.getWebsite(),
        institute.getAddressLine1(),
        institute.getAddressLine2(),
        institute.getCity(),
        institute.getState(),
        institute.getPostalCode(),
        institute.getCountry(),
        institute.getIconUrl());
  }
}
