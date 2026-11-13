package com.ims.identity.application;

import com.ims.common.tenancy.TenantContext;
import com.ims.identity.api.ManagedUserResponse;
import com.ims.identity.api.RoleResponse;
import com.ims.identity.domain.Role;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.RoleRepository;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserManagementService {

  private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);
  private static final Set<String> ASSIGNABLE_ROLES =
      Set.of(
          "ADMIN",
          "ACCOUNTANT",
          "ACADEMIC_COORDINATOR",
          "FACULTY",
          "FRONT_DESK",
          "STUDENT");

  private final UserAccountRepository userAccountRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;

  public UserManagementService(
      UserAccountRepository userAccountRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService) {
    this.userAccountRepository = userAccountRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
  }

  public record CreateUserCommand(
      @NotBlank @Size(max = 64) String username,
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotEmpty List<@NotBlank String> roles) {}

  public record UpdateUserCommand(
      @NotBlank @Email @Size(max = 255) String email,
      @NotEmpty List<@NotBlank String> roles,
      @Size(min = 8, max = 72) String password) {}

  @Transactional(readOnly = true)
  public List<RoleResponse> listAssignableRoles() {
    requireTenant();
    return roleRepository.findAll().stream()
        .filter(r -> ASSIGNABLE_ROLES.contains(r.getCode()))
        .sorted(Comparator.comparing(Role::getCode))
        .map(r -> new RoleResponse(r.getCode(), r.getName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ManagedUserResponse> list() {
    long instituteId = requireTenant();
    return userAccountRepository
        .findByInstituteIdAndDeletedAtIsNullOrderByUsernameAsc(instituteId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ManagedUserResponse get(Long id) {
    return toResponse(requireUser(id));
  }

  @Transactional
  public ManagedUserResponse create(CreateUserCommand command) {
    long instituteId = requireTenant();
    String username = command.username().trim();
    String email = command.email().trim().toLowerCase();
    Set<Role> roles = resolveAssignableRoles(command.roles());

    if (userAccountRepository.findByUsernameAndInstituteId(username, instituteId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }
    if (userAccountRepository.findByEmailAndInstituteId(email, instituteId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    UserAccount saved =
        userAccountRepository.save(
            new UserAccount(
                instituteId, username, email, passwordEncoder.encode(command.password()), roles));
    auditService.record(
        instituteId,
        "identity",
        "User",
        saved.getId(),
        "USER_CREATED",
        null,
        Map.of("username", saved.getUsername(), "roles", roleCodes(saved)),
        null);
    log.info("User created id={} username={} instituteId={}", saved.getId(), username, instituteId);
    return toResponse(saved);
  }

  @Transactional
  public ManagedUserResponse update(Long id, UpdateUserCommand command) {
    long instituteId = requireTenant();
    UserAccount user = requireUser(id);
    Map<String, Object> before =
        Map.of("email", user.getEmail(), "roles", roleCodes(user), "status", user.getStatus());

    String email = command.email().trim().toLowerCase();
    userAccountRepository
        .findByEmailAndInstituteId(email, instituteId)
        .filter(other -> !other.getId().equals(user.getId()))
        .ifPresent(
            other -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            });

    Set<Role> roles = resolveAssignableRoles(command.roles());
    assertNotRemovingLastAdmin(instituteId, user, roles);

    user.updateEmail(email);
    user.replaceRoles(roles);
    if (command.password() != null && !command.password().isBlank()) {
      user.changePassword(passwordEncoder.encode(command.password()));
    }
    userAccountRepository.save(user);

    auditService.record(
        instituteId,
        "identity",
        "User",
        user.getId(),
        "USER_UPDATED",
        before,
        Map.of("email", user.getEmail(), "roles", roleCodes(user), "status", user.getStatus()),
        null);
    return toResponse(user);
  }

  @Transactional
  public ManagedUserResponse deactivate(Long id) {
    long instituteId = requireTenant();
    UserAccount user = requireUser(id);
    assertNotSelf(user.getId(), "You cannot deactivate your own account");
    assertNotRemovingLastAdmin(instituteId, user, Set.of());
    Map<String, Object> before = Map.of("status", user.getStatus(), "roles", roleCodes(user));
    user.deactivate();
    userAccountRepository.save(user);
    auditService.record(
        instituteId,
        "identity",
        "User",
        user.getId(),
        "USER_DEACTIVATED",
        before,
        Map.of("status", user.getStatus()),
        null);
    return toResponse(user);
  }

  @Transactional
  public ManagedUserResponse activate(Long id) {
    long instituteId = requireTenant();
    UserAccount user = requireUser(id);
    Map<String, Object> before = Map.of("status", user.getStatus());
    user.activate();
    userAccountRepository.save(user);
    auditService.record(
        instituteId,
        "identity",
        "User",
        user.getId(),
        "USER_ACTIVATED",
        before,
        Map.of("status", user.getStatus()),
        null);
    return toResponse(user);
  }

  private void assertNotSelf(Long userId, String message) {
    Long actorId = currentUserId();
    if (actorId != null && actorId.equals(userId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }

  private void assertNotRemovingLastAdmin(long instituteId, UserAccount user, Set<Role> nextRoles) {
    boolean wasAdmin =
        user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()))
            && "ACTIVE".equals(user.getStatus());
    boolean willBeAdmin =
        nextRoles.stream().anyMatch(r -> "ADMIN".equals(r.getCode()))
            && "ACTIVE".equals(user.getStatus());
    // Deactivate path passes empty nextRoles → willBeAdmin false while wasAdmin may be true
    if (!wasAdmin) {
      return;
    }
    if (willBeAdmin) {
      return;
    }
    long otherAdmins = userAccountRepository.countActiveAdmins(instituteId, user.getId());
    if (otherAdmins == 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Cannot remove or deactivate the last ACTIVE ADMIN");
    }
  }

  private Set<Role> resolveAssignableRoles(List<String> roleCodes) {
    Set<Role> roles = new HashSet<>();
    for (String raw : roleCodes) {
      String code = raw.trim().toUpperCase();
      if (!ASSIGNABLE_ROLES.contains(code)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Role not assignable: " + code);
      }
      Role role =
          roleRepository
              .findByCode(code)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + code));
      roles.add(role);
    }
    return roles;
  }

  private UserAccount requireUser(Long id) {
    long instituteId = requireTenant();
    return userAccountRepository
        .findByIdAndInstituteIdAndDeletedAtIsNull(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private static Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ImsUserPrincipal p)) {
      return null;
    }
    return p.getUserId();
  }

  private static List<String> roleCodes(UserAccount user) {
    return user.getRoles().stream()
        .map(Role::getCode)
        .sorted()
        .collect(Collectors.toList());
  }

  private ManagedUserResponse toResponse(UserAccount user) {
    return new ManagedUserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getStatus(),
        roleCodes(user),
        user.getLastLoginAt(),
        user.getCreatedAt());
  }
}
